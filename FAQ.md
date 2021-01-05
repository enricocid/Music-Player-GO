# Music-Player-GO - Frequently Asked Questions (FAQ)

For a list of **supported media formats** and related issues, please refer to [supported formats page](https://github.com/enricocid/Music-Player-GO/blob/master/FORMATS.md).

This FAQ will be continually updated to accommodate new information with respect to new updates and changes on the app, and will be used inside the app to help users find answers to their questions.


### What is Music Player GO?

It is a lightweight, original, privacy-friendly, and **ads-free** music player that plays music from your local music library. It has a colorful and extremely simple UI.


### Where can I download Music Player GO?

Music Player GO can be downloaded [here](https://play.google.com/store/apps/details?id=com.iven.musicplayergo) on the Google Play Store, and [here](https://f-droid.org/packages/com.iven.musicplayergo/) on F-Droid. 


### How can I contribute to translations?

Join us on [Weblate](https://hosted.weblate.org/engage/music-player-go/). :)


### Can I customize the app's theme to my taste?

1. Yes, you can choose between 3 themes:

 - Light
 - Dark
 - Automatic: this mode will follow system settings in Android Q and battery levels in pre-Q as suggested in the [guidelines](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme).

2. 19 colorful accents... sounds good?


### Where is the deep black dark theme?

Deep black dark theme is not available. To express elevation and depth, it is highly recommended to "darken with grey rather than black".
Moreover, **accessibility** has to be taken into account.
Having said that, feature requests asking for deep black theme won't be considered. Guidelines can be found [here](https://material.io/design/color/dark-theme.html).


### Why are accents 'lighter' in the dark theme?

As per [guidelines](https://material.io/design/color/dark-theme.html), primary colors are desaturated for **accessibility**: saturated colors also produce optical vibrations against a dark background, which can induce eye strain.
Instead, desaturated colors can be used as a more legible alternative.


### Does Music Player GO use an equalizer?

Yes, Music Player GO uses the system (default) equalizer (if it exists) found on the device or the built-in one.
The equalizer can be found in the "Now Playing" dialog.


### Is there a built-in equalizer?

Yes, Music Player GO has a built-in equalizer. It is similar to the stock equalizer and it is intended for devices not provived with a system equalizer. If You want to use this one please disable the system equalizer from system settings.


### The built-in equalizer is not being enabled

Just toggle the switch button again.


### What is audio focus?

Audio focus is an Android feature to moderate media app's playback and prevent multiple media app to play at the same time.
Every media app should request and receive the audio focus. This option is enabled by default so that MPGO can:
1. **pause** if another app requests audio focus.
2. **lower** its volume when a new message is received.


### How to clear queued or loved songs?

Just long click on the buttons from the bottom panel.
To clear the queue You can also start playback from artist's details.


### Does Music Player GO play music tracks from online sources?

No, Music Player GO doesn't play music tracks from any online source.
It only plays from the local library.


### Does Music Player GO access the internet?

No, Music Player GO does not access the internet.
The internet permission is not required by this app.


### How does Music Player GO categorized music tracks?

Music is categorized by artists, folders and albums.
Artists' music are in turn categorized by albums.
Folders can be considered albums.
Each album consists of tracks of the same album.


### Can I exclude folders from my library?

You can filter them once the app has loaded.


### Why does shuffled music is not added to queue?

To avoid performance issues queue built from shuffled song is actually limited to 50 items.


### What about features requests/crash reports?

1. Not all requests can be taken into considerationk unfortunately. This could happen for a variety of reasons:

   - The feature is too complex, clashing with the philosophy behind the app: KISS (keep it simple, stupid)
   - The developer is kinda poor and can't afford resources like real devices needed for testing purposes (a dying Nexus 5 being his testing device for now), or a good computer that can ensure emulator usage. This is the case of issues [#29](https://github.com/enricocid/Music-Player-GO/issues/29) and [#51](https://github.com/enricocid/Music-Player-GO/issues/51).
   - The feature just does not match the developer's needs. Keep in mind that this app is built around the developer's usage needs and that it was shared for the love of open-source.
   - The developer is a chemist with an unstable work condition in real life, sometimes time is just short...
   - Lack of motivation/interest

2. A crash report is useful only when a logcat and detailed explanation on how to reproduce the bug is provided. If you don't know how to proceed, just send an email to the developer to receive guidance.


### Does this app keeps any information or data about me or my device?

**Information or data is not kept by this app**. Neither the device's nor owner's information is obtained by this app.


If you have other questions that are not listed in the above FAQ, kindly ask by creating an issue.
We will be available to give answers to your questions.
