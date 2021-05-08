package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"github.com/imroc/req"
)

func getStdin() (input string) {
	scanner := bufio.NewScanner(os.Stdin)

	scanner.Scan()
	input = scanner.Text()

	input = strings.TrimSpace(input)
	return
}

type APIChannels []struct {
	ID                   string      `json:"id"`
	Type                 int         `json:"type"`
	Name                 string      `json:"name"`
	Position             int         `json:"position"`
	ParentID             interface{} `json:"parent_id"`
	GuildID              string      `json:"guild_id"`
	PermissionOverwrites []struct {
		ID       string `json:"id"`
		Type     string `json:"type"`
		Allow    int    `json:"allow"`
		Deny     int    `json:"deny"`
		AllowNew string `json:"allow_new"`
		DenyNew  string `json:"deny_new"`
	} `json:"permission_overwrites"`
	Nsfw             bool        `json:"nsfw"`
	LastMessageID    string      `json:"last_message_id,omitempty"`
	LastPinTimestamp time.Time   `json:"last_pin_timestamp,omitempty"`
	Topic            string      `json:"topic,omitempty"`
	RateLimitPerUser int         `json:"rate_limit_per_user,omitempty"`
	Bitrate          int         `json:"bitrate,omitempty"`
	UserLimit        int         `json:"user_limit,omitempty"`
	RtcRegion        interface{} `json:"rtc_region,omitempty"`
	VideoQualityMode int         `json:"video_quality_mode,omitempty"`
}

type APIRoles []struct {
	ID             string `json:"id"`
	Name           string `json:"name"`
	Permissions    int    `json:"permissions"`
	Position       int    `json:"position"`
	Color          int    `json:"color"`
	Hoist          bool   `json:"hoist"`
	Managed        bool   `json:"managed"`
	Mentionable    bool   `json:"mentionable"`
	PermissionsNew string `json:"permissions_new"`
	Tags           struct {
		PremiumSubscriber interface{} `json:"premium_subscriber"`
		BotID             string      `json:"bot_id"`
	} `json:"tags,omitempty"`
}

func main() {
	fmt.Print("Discord Token: ")
	discordToken := getStdin()

	fmt.Print("Discord Server(Guild) ID: ")
	discordGuildId := getStdin()

	// Save token & guild_id
	fileConfig, errC := os.Create("../run/config.json")
	if errC != nil {
		log.Fatal(errC)
	}
	defer fileConfig.Close()

	configMap := make(map[string]string)
	configMap["token"] = discordToken
	configMap["guild_id"] = discordGuildId
	jsonConfig, _ := json.Marshal(configMap)
	_, write_config_err := fileConfig.Write(jsonConfig)
	if write_config_err != nil {
		log.Fatal(write_config_err)
	}

	// Generate define channel ids
	channels := getChannels(discordToken, discordGuildId)
	channelsMap := make(map[string]string)
	for _, channel := range channels {
		if channel.Type != 0 {
			continue
		}
		channelsMap[channel.Name] = channel.ID
	}

	// Generate define role ids
	roles := getRoles(discordToken, discordGuildId)
	rolesMap := make(map[string]string)
	for _, role := range roles {
		name := strings.Replace(role.Name, "*", "", -1)
		rolesMap[name] = role.ID
	}
	defineMap := make(map[string]map[string]string)
	defineMap["channels"] = channelsMap
	defineMap["roles"] = rolesMap

	file, err := os.Create("../run/defines.json")
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	jsonStr, _ := json.Marshal(defineMap)
	_, write_err := file.Write(jsonStr)
	if write_err != nil {
		log.Fatal(write_err)
	}

	fmt.Print("Generated. Enter to exit")
	getStdin()
}

func getChannels(discordToken string, discordGuildId string) (result APIChannels) {
	header := req.Header{
		"Content-Type":  "application/json",
		"Authorization": "Bot " + discordToken,
	}
	r, err := req.Get("https://discord.com/api/guilds/"+discordGuildId+"/channels", header)
	if err != nil {
		log.Fatal(err)
	}
	r.ToJSON(&result)
	return
}

func getRoles(discordToken string, discordGuildId string) (result APIRoles) {
	header := req.Header{
		"Content-Type":  "application/json",
		"Authorization": "Bot " + discordToken,
	}
	r, err := req.Get("https://discord.com/api/guilds/"+discordGuildId+"/roles", header)
	if err != nil {
		log.Fatal(err)
	}
	r.ToJSON(&result)
	return
}
