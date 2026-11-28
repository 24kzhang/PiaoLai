import argparse
import fcntl
import re
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor
from functools import partial
from pathlib import Path
from threading import local

import redis
import requests
import yaml


DIRECTORY = Path(__file__).resolve().parent
OUTPUT = DIRECTORY / "tokens.csv"
BASE_URL = "http://127.0.0.1:8081"
FIRST_PHONE = 19900000000
MAX_USERS = 100_000
BATCH_SIZE = 2_000
TOKEN_PATTERN = re.compile(r"[0-9a-f]{32}")
CLIENT = local()


def request(path, method="POST", **kwargs):
    if not hasattr(CLIENT, "session"):
        CLIENT.session = requests.Session()
        CLIENT.session.trust_env = False
    with CLIENT.session.request(method, BASE_URL + path, timeout=(3, 15), **kwargs) as response:
        response.raise_for_status()
        result = response.json()
    if result.get("success") is not True:
        raise ValueError(f"{path} 返回业务失败")
    return result.get("data")


def issue_token(cache, offset):
    phone = str(FIRST_PHONE + offset)
    for attempt in range(3):
        try:
            request("/user/code", params={"phone": phone})
            code = cache.get(f"login:code:{phone}")
            if not code or len(code) != 6 or not code.isdigit():
                raise ValueError("本地 Redis 中没有有效的测试验证码")

            token = request("/user/login", json={"phone": phone, "code": code})
            if not isinstance(token, str) or not TOKEN_PATTERN.fullmatch(token):
                raise ValueError("登录接口未返回有效令牌")
            return token
        except (requests.RequestException, redis.RedisError, ValueError):
            if attempt == 2:
                raise
            time.sleep(0.25 * (attempt + 1))


def verify_batch(cache, tokens, seen_tokens, seen_users):
    if len(set(tokens)) != len(tokens) or seen_tokens.intersection(tokens):
        raise ValueError("令牌出现重复")
    if any(not TOKEN_PATTERN.fullmatch(token) for token in tokens):
        raise ValueError("令牌格式异常")

    with cache.pipeline(transaction=False) as pipeline:
        for token in tokens:
            pipeline.hget(f"login:token:{token}", "id")
        user_ids = pipeline.execute()
    if any(user_id is None or not user_id.isdecimal() for user_id in user_ids):
        raise ValueError("有令牌缺少有效的登录状态")
    if len(set(user_ids)) != len(tokens) or seen_users.intersection(user_ids):
        raise ValueError("用户 ID 出现重复")

    seen_tokens.update(tokens)
    seen_users.update(user_ids)


def save(tokens):
    staged = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", dir=DIRECTORY, prefix="tokens-", suffix=".tmp",
            encoding="utf-8", delete=False,
        ) as output:
            staged = Path(output.name)
            output.write("\n".join(tokens) + "\n")
        staged.replace(OUTPUT)
    finally:
        if staged is not None:
            staged.unlink(missing_ok=True)


def fill(cache, count, worker_count):
    tokens = OUTPUT.read_text(encoding="utf-8").splitlines() if OUTPUT.exists() else []
    if len(tokens) > count:
        raise ValueError(f"文件已有 {len(tokens):,} 条，目标数量不能少于现有数量")

    seen_tokens, seen_users = set(), set()
    for start in range(0, len(tokens), BATCH_SIZE):
        verify_batch(cache, tokens[start:start + BATCH_SIZE], seen_tokens, seen_users)
    print(f"已核验现有 {len(tokens):,} 枚令牌，目标 {count:,} 枚。", flush=True)

    login = partial(issue_token, cache)
    with ThreadPoolExecutor(max_workers=worker_count) as workers:
        for start in range(len(tokens), count, BATCH_SIZE):
            end = min(start + BATCH_SIZE, count)
            batch = list(workers.map(login, range(start, end)))
            verify_batch(cache, batch, seen_tokens, seen_users)
            tokens.extend(batch)
            save(tokens)
            print(f"已保存 {end:,}/{count:,} 枚独立令牌。", flush=True)


def main():
    parser = argparse.ArgumentParser(description="为本地抢票项目补充测试账号登录令牌")
    parser.add_argument("--count", type=int, default=1000, help="令牌总数，默认 1000")
    parser.add_argument("--workers", type=int, default=24, help="登录并发数，默认 24")
    args = parser.parse_args()
    if not 1 <= args.count <= MAX_USERS:
        parser.error("令牌总数须在 1 至 100000 之间")
    if not 1 <= args.workers <= 48:
        parser.error("登录并发数须在 1 至 48 之间")

    try:
        config_path = DIRECTORY.parent / "src/main/resources/application.yaml"
        config = yaml.safe_load(config_path.read_text(encoding="utf-8"))["spring"]["redis"]
        if config["host"] not in {"127.0.0.1", "localhost", "::1"}:
            raise ValueError("请使用项目的本地测试 Redis")
        with (DIRECTORY / "tokens.lock").open("a") as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                raise ValueError("已有令牌生成任务正在运行") from None
            with redis.Redis(
                host=config["host"],
                port=config["port"],
                db=config.get("database", 0),
                username=config.get("username"),
                password=config.get("password"),
                decode_responses=True,
                socket_timeout=5,
                socket_connect_timeout=5,
            ) as cache:
                cache.ping()
                fill(cache, args.count, args.workers)
    except (requests.RequestException, redis.RedisError, yaml.YAMLError, OSError, KeyError) as error:
        parser.exit(1, f"生成失败（{type(error).__name__}）。已保存的批次可以续跑。\n")
    except ValueError as error:
        parser.exit(1, f"生成失败（{error}）。已保存的批次可以续跑。\n")

    print(f"已验证 {args.count:,} 个独立账号的登录令牌。")


if __name__ == "__main__":
    main()
