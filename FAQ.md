# Music-Player-GO - Frequently Asked Questions (FAQ)

For a list of **supported media formats** and related issues, please refer to [supported formats page](https://github.com/enricocid/Music-Player-GO/blob/master/FORMATS.md).

This FAQ will be continually updated to accommodate new information with respect to new updates and changes on the app, and will be used to help users find answers to their questions.


### What is Music Player GO?

A lightweight, original, privacy-friendly, **ad-free** music player playing music from your local music library, in acolorful and simple UI.


### Where can I download Music Player GO?

[Here](https://f-droid.org/packages/com.iven.musicplayergo/) on F-Droid.
[Here](https://play.google.com/store/apps/details?id=com.iven.musicplayergo) on the Google Play Store.

### How can I contribute to translations?

Join us on [Hosted Weblate](https://hosted.weblate.org/engage/music-player-go/). :)


### Can I customise the app's theme to my taste?

1. Yes, choose between 3 themes:

 - Light
 - Dark
 - Automatic: still follows system settings in Android Q and battery levels in pre-Q as suggested in the [guidelines](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme).

2. 36 accent colours.

### Where is the deep black dark theme?

Deep black dark theme is not available. To express elevation and depth, it is highly recommended to "darken with grey rather than black".
Moreover, accessibility has to be taken into account.
That said, feature requests for it doesn't change matters. The guidelines can be found [here](https://material.io/design/color/dark-theme.html).

### Why are accents 'lighter' in the dark theme?

As per [guidelines](https://material.io/design/color/dark-theme.html), primary colors are desaturated for accessibility: saturated colors also produce optical vibrations against a dark background, which can induce eye strain.
Instead, desaturated colors can be used as a more legible alternative.


### Does Music Player GO have or use an equaliser?

Music Player GO uses the system (default) equaliser (if it exists) found on the device.
The equaliser can be found in the "Now Playing" dialog.
There is also a built-in equalizer. You can use by turning off the one in your system, or if your system doesn't have one.


### The built-in equalizer is not being turned on

Just toggle the switch button again.


### What is audio focus?

It is an Android feature to moderate playback in media apps and prevents multiple media apps from playing at the same time.
Every media app should request and receive the audio focus. This option is on by default so that MPGO can:
1. **pause** if another app requests audio focus.
2. **lower** its volume when a new message is received.


### How to clear queued or loved songs?

Long-click on the buttons from the bottom panel.
To clear the queue You can also start playback from "Artist" details.

### Does Music Player GO play music tracks from online sources?

No, it only plays from the local library.

### Does Music Player GO access the Internet?

No.
Internet permission is not requested.


### How does Music Player GO categorized music tracks?

By artists, folders, and albums.
Artists' music are in turn categorized by albums.
Folders can be considered albums.
Each album consists of tracks of the same album.


### Can I exclude folders from my library?

Filter them once the app has loaded.


### Why is shuffled music not added to my queue?

To avoid performance issues queue built from shuffled songs is actually limited to 1000 items.


### What about features requests/crash reports?

1. Unfortunately, not all requests can be taken into consideration, for a variety of reasons:

   - The feature is too complex, clashing with the philosophy behind the app: KISS. Keep it snimple snupid :)
   - The rather poor developer is rocking a dying Nexus 5 as the only test device for now. A good computer could ensure emulator usage. This is the case of issues [#29](https://github.com/enricocid/Music-Player-GO/issues/29) and [#51](https://github.com/enricocid/Music-Player-GO/issues/51).
   - The feature just does not match the developer's needs. Keep in mind that this app is built around the developer's usage needs and is shared for the love of libre software.
   - The developer is a chemist with an unstable work condition in real life, sometimes time is just short…
   - Lack of motivation/interest/you didn't do it yourself

2. A crash report is useful only when a logcat and detailed explanation on how to reproduce the bug is provided. If you don't know how to proceed, send an e-mail to the developer to receive guidance.


### Does this app keep any info or data about me or my device?

No. Neither the device's nor owner info.

Pose other questions by creating issues, and we will answer them.
