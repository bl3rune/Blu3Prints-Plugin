# Blu3print Plugin

[![Latest Release](https://img.shields.io/github/v/release/bl3rune/Blu3Prints-Plugin)](https://github.com/bl3rune/Blu3Prints-Plugin/releases)
[![Build Status](https://github.com/bl3rune/Blu3Prints-Plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/bl3rune/Blu3Prints-Plugin/actions)
![Minimum MC Version](https://img.shields.io/badge/Spigot_Versions-v1.18_--_v1.21.8-orange)

![Banner](https://github.com/bl3rune/Blu3Prints-Plugin/raw/main/images/New-Banner.png "Banner")
### **A blueprint tool that allows players to capture/share/manipulate designs within Minecraft** 
### Blu3prints are placed using blocks in inventory in survival (or free in creative mode / with permissions)


![usage!](https://github.com/bl3rune/Blu3Prints-Plugin/raw/main/images/Blu3print.gif "Usage")

## Video tutorials playlist
[![Watch the video](https://img.youtube.com/vi/iNgYVwC9tRA/maxresdefault.jpg)](https://www.youtube.com/watch?v=iNgYVwC9tRA&list=PLAnUQqo3m_n1ULxKJXg3neW2GYDa3U3Sj)

## Features
- Craftable Blu3print writer item with customisable recipe
- Area selection method to copy block within selection
- Blu3print Writer is based on writable book and so encoded Blu3prints can be imported directly using the book
- Blu3prints are nameable to allow for at-a-glance visibility
- Blu3print UUID structure allows less overhead for server operators for storing large quantities of blu3prints
- Blu3prints can be explained in chat by right clicking on it
- Ability for any Blu3print to be exported and shared as copyable text; e.g.  `B=STONE|2:3:1|E-0-1~BA|B|`
- Shared Blu3prints can be imported by other people on the same server, or even on entirely different servers running this plugin. Allowing players to preserve their favourite creations and rebuild them on other servers with ease!
- Blu3print items can be manipulated in a number of ways
    - **Rotated around blockface**
    - **Change blockface direction**
    - **Adjusting scale** (limitable)
    - **Renamed**
    - **Duplicated**
- Blu3prints can also be manipulated from cartography table
- Configuration allows for safeguards on usage and caps on blueprint sizes
- Blu3print writer has a cooldown to prevent spam
- Permissions to fine tune usage of blu3prints
- In-Game `/blu3print.help` command to guide new users

## Getting started
[See the wiki getting started for detailed set up.](https://github.com/bl3rune/Blu3Prints-Plugin/wiki/Getting-Started)

First start by crafting a Blu3print Writer in the crafting table using these materials in any configuration:

PAPER, LAPIS_LAZULI, FEATHER

While holding a Blu3print Writer, you can interact by:
- Using left click on a block to set the first position of the selection area (this clears the ignore list if there is one)
- Using left click (while sneaking) on a block to ignore/unignore the block within a selection
- Using right click on a block to set the second position of the selection area (this clears the ignore list if there is one)
- Using right click (while sneaking) on a block to show the ignore block list within the current selection
- Using right click on the air to set open the blu3print writer

With the blu3print writer open:
- Click on sign and give the blu3print a name to complete it
- Or instead of using area selection, enter a blu3print code on the pages of the book and then sign and complete

While holding a completed Blu3print, you can interact by:
- Using left click on a block to build the blu3print on top of that block
- Using right click on the air to print an explanation of the blu3print to chat like below
- Using right click on a block to place a holographic preview


### Exporting & Importing
------
[See the wiki sharing Blu3prints pages for how to get set up.](https://github.com/bl3rune/Blu3Prints-Plugin/wiki/Sharing-Blu3prints-and-using-between-servers)

Blu3prints can be exported by using a cartography table or by using the `blu3print.export` command whilst holding the blu3print item.
![Export](https://github.com/bl3rune/Blu3Prints-Plugin/raw/main/images/Export.png "Export")
This code can be shared to other players and even be used on other servers running this plugin, allowing you to preserve your creations across servers or even showcase them online (visualiser website in the pipeline!)
To use the encoded blu3prints, simply run the command `/blu3print.import <name> <encoded-string>`


## Permissions
By default all commands apart from `/blu3print.give` should be available for all non OP players.
There are also a default restrictions on the maximum size/scale of a blu3print for safety reasons. This can be configured in the `config.yml` file. 

[See the wiki permissions page for how to get set up](https://github.com/bl3rune/Blu3Prints-Plugin/wiki/Permissions)

![Safety](https://github.com/bl3rune/Blu3Prints-Plugin/raw/main/images/Safety.png "Safety")


## Help
Players can use the `/blu3print.help` command to get help with usage in-game or [look at the wiki pages](https://github.com/bl3rune/Blu3Prints-Plugin/wiki)

![Help](https://github.com/bl3rune/Blu3Prints-Plugin/raw/main/images/Help.png "Help")


## Configuration
This plugin is fairly plug-and-play, simply place inside the `plugins` folder of your server and start 
the server. 

Configuration can be found in the `config.yml` file.

[See the wiki configuration pages for how to get set up](https://github.com/bl3rune/Blu3Prints-Plugin/wiki/Configuration)


## License
This project is licensed under the MIT License; 
see [MIT License](LICENSE) for details.