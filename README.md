# Clubhot: Get Beatport Top 100 Analysis in Spotify

![ClubHot Demo](/public/images/demo.png)

## What is Clubhot about?

Clubhot is a pet project of mine that allows you to get the Beatport top 100 right into
a Spotify playlist of your choice and use
the [Spotify Audio Feature](https://developer.spotify.com/documentation/web-api/reference/get-audio-features) to run an
analysis!

## Tech Stack

Clubhot uses 
- **Scala Play Framework** 
- **Spotify API**
- **JSoup** 
- **HTML/CSS**

## How to setup Clubhot?

### The Prerequisites

- Begin by cloning the project.
- Make sure to have Java installed, I am using Java 11 and Scala 2.13.10 (as setup in build.sbt)

    - The time I was developing this project, Play was having problems with Scala 3 and Java 17.

### Configuration

As in Oauth, you have to register the client (this program) with Spotify and get Client ID and
Client Secret. This will allow the server to call out to the Spotify API.

Here are the values you can configure under `conf/application.conf`:
- *spotify.api.clientid*: Client ID you can from Spotify Developer Portal
- *spotify.api.secret*: Client Secret you can from Spotify Developer Portal
- *token.encryption.key*: The key that is used for encrypting the client secret and id in the browser. Can be any string, but make sure it is set
- *spotify.api.callback*: The callback where Spotify will use after the Resource Owner authentication.
- *spotify.playlist* (OPTIONAL): The playlist ID that will be updated with the Beatport Top 100 songs. Please create the
playlist first using the Spotify UI and get the ID.

### Running

- In the root of the project, run: `sbt`
- In sbt prompt, run: `run`
- The server will run in localhost:9000, you can change that (make sure to also update the callback URL for OAuth)