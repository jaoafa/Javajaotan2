import argparse
import os
import time
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor, as_completed

import markovify

parser = argparse.ArgumentParser(description='gentextコマンドで使用するマルコフ連鎖による文章生成スクリプト')
parser.add_argument(
    "--source",
    default="default.json",
    help='文章ソースとするJSONファイル'
)
parser.add_argument(
    "--count",
    type=int,
    default=1,
    help='いくつ文章を生成するか',
)
parser.add_argument(
    "--long-generate",
    action='store_false',
    help='文章生成時に文字数制限をしないか',
)


def generate(model, long_generate=False):
    if long_generate:
        sentence = model.make_sentence()
    else:
        sentence = model.make_short_sentence(140)

    if sentence is None:
        if long_generate:
            sentence = model.make_sentence()
        else:
            sentence = model.make_short_sentence(140)

    if sentence is None:
        return None

    return "".join(sentence.split())


if __name__ == '__main__':
    before_time = time.time()

    args = parser.parse_args()
    filename = args.source
    generate_count = args.count
    if generate_count > 100:
        print("[ERROR] CANNOT SET GENERATE COUNT MORE THAN 100.")
        exit(1)

    if not filename.endswith(".json"):
        filename = filename + ".json"

    if "/" in filename:
        print("[ERROR] SOURCE FILE NAME IS NOT VALID.")
        exit(1)

    path = os.path.dirname(os.path.abspath(__file__)) + "/sources/" + filename

    if not os.path.exists(path):
        print("[ERROR] SOURCE FILE ({}) IS NOT FOUND.".format(filename))
        exit(1)

    with open(path, "r") as f:
        text_model = markovify.NewlineText.from_json(f.read())

    if generate_count == 1:
        text = None
        while text is None:
            if args.long_generate:
                text = text_model.make_sentence()
            else:
                text = text_model.make_short_sentence(140)
        texts = ["".join(text.split())]
    else:
        with ProcessPoolExecutor() as executor:
            futures = []
            for i in range(generate_count):
                futures.append(executor.submit(generate, text_model, args.long_generate))

            texts = [f.result() for f in as_completed(futures)]

    countFailed = len(list(filter(lambda x: x is None, texts)))
    countDuplicated = len(list(set(filter(lambda x: x is not None, texts)))) - len(list(filter(lambda x: x is not None, texts)))

    texts = set(filter(lambda x: x is not None, texts))

    print("```")
    if len("\n".join(texts)) <= 1900:
        print("\n".join(texts))
    else:
        print("\n".join(texts)[:1900])

    print("")

    if len("\n".join(texts)) >= 1900:
        print("※Discordの文字数制限回避のため、一部の結果が削られています。")

    if countFailed > 0:
        print("※" + str(countFailed) + "回、生成に失敗しました。")

    if countDuplicated > 0:
        print("※" + str(countDuplicated) + "回、被りました。")

    after_time = time.time()
    print("※処理時間: " + str(format(after_time - before_time, '.3f')) + "秒")

    print("```")
