import argparse
import json
import os

import requests

parser = argparse.ArgumentParser(description='Guildメンバーを取得します')
parser.add_argument(
    "--guild-id",
    required=True,
    help='メンバーを取得する対象のGuild Id'
)

def load_config():
    if not os.path.exists("config.json"):
        return False
    with open("config.json") as f:
        return json.load(f)


def get_guild_members(config, guild_id: str):
    response = requests.get("https://discord.com/api/guilds/%s/members?limit=1000" % guild_id, headers={
        "Authorization": "Bot %s" % config["token"]
    })
    return response.json()


def main(args: argparse.Namespace):
    config = load_config()
    if config is False:
        print("[ERROR] config.json is not found")
        exit(1)

    guildId = args.guild_id
    members = get_guild_members(config, guildId)
    print(json.dumps(members), end="")


if __name__ == '__main__':
    main(parser.parse_args())
