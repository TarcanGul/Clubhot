## Clubhot

Get the features of Top 100 Beatport songs using spotify api and display in Web UI!
Also updates the Beatport Top 100 playlist automatically!

Decided to seperate token and spotify service.

*Steps*:
- Authorize to Spotify account
- Put user info to the screen with button under it
  - When the button is pressed, get info about top 100 beatport songs (genres for later maybe)
    - Pull the songs using JSoup
      - Get Song Title, Artists, Remixers (enough to identify)
      - Is there a way to get spotify song id from title (maybe search)
    - Internally, let's have an updated playlist. We can then pull songs from the playlist using Spotify API
      - Analyze the songs in the playlist and display the results

Goals:
- Use scala best practices to make a fast responsive web app using scala play. 

Notes:
- Runs with Java 11 because of Scala Play compatibility issues with Java 17.

    