import argparse
import os
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


if __name__ == '__main__':
    args = parser.parse_args()
    filename = args.source
    generate_count = args.count
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

    texts = []
    countFailed = 0
    countDuplicated = 0
    for i in range(generate_count):
        sentence = text_model.make_sentence()
        if sentence is None:  # リトライ
            if generate_count == 1:
                while sentence is None:
                    sentence = text_model.make_sentence()
            else:
                sentence = text_model.make_sentence()
                if sentence is None:
                    countFailed += 1  # リトライしてダメだったらやめとく
                    continue

        if ''.join(sentence.split()) in texts:
            countDuplicated += 1
            continue

        texts.append(''.join(sentence.split()))

    print("```")
    if len("\n".join(texts)) <= 1900:
        print("\n".join(texts))
    else:
        print("\n".join(texts)[:1900])

    if len("\n".join(texts)) >= 1900 or countFailed > 0 or countDuplicated > 0:
        print("")

    if len("\n".join(texts)) >= 1900:
        print("※Discordの文字数制限回避のため、一部の結果が削られています。")

    if countFailed > 0:
        print("※" + str(countFailed) + "回、生成に失敗しました。")

    if countDuplicated > 0:
        print("※" + str(countFailed) + "回、被りました。")

    print("```")
