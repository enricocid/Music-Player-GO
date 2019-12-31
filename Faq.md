# Music-Player-GO - Frenquetly Asked Questions (FAQ)

For a list of **supported media formats** and related issues please refer to [supported formats page](https://github.com/enricocid/Music-Player-GO/blob/master/Formats.md).

This frequently asked questions will be continually updated to accommodate new information with respect to new updates and changes on the app and will be used inside the app to help users find answers to their questions.


### What is Music Player GO?

It is a lightweight, original, privacy-friendly and **ads-free** music player that plays music from local library. It has a colorful and extremely simple UI.


### Where can I download Music Player GO?

Music Player GO can be downloaded [here](https://play.google.com/store/apps/details?id=com.iven.musicplayergo) on the Google Play Store and [here](https://f-droid.org/repository/browse/?fdid=com.iven.musicplayergo) on F-Droid. 


### Sometimes app crashes when opened from background

It could happens on devices with Android versions >= Marshmallow because of the doze mode. Developers are not allowed to setup apps to ignore battery's system optimizations. All You can do is to disable it by yourself:

Music Player GO app's info -> Battery -> Battery optimization -> Music Player GO -> Don't optimize


### Sometimes music is not fully loaded

This is not Music Player GO fault. Every time the app is restarted MediaStore is queried. After many accesses to MediaStore databases for some reasons (probably security) the query fails. This problem is present also on any app implementing querying music on boot (e.g. Phonograph, Shuttle Music Player).
Moral: limit the app restarts.


### Can I customize the app's theme to my taste?

1. Yes, you can choose between 3 themes:

 - Light
 - Dark
 - Automatic: this mode will follow system settings in Android Q and battery levels on pre-Q as suggested in [guidelines](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme)

2. 19 colorful accents ... sounds good?


### Where is the deep black dark theme?

Deep black dark theme is not available. To express elevation and depth it is highly recommended to "darken with grey rather than black". So, dark theme is not only about blindly inverting the UI for users happiness. Moreover, **accessibility** has to be taken into account. Having said that, feature requests asking for deep black theme won't be considered. Guidelines can be found [here](https://material.io/design/color/dark-theme.html)


### Why accents are 'lighter' in dark theme?

As per [guidelines](https://material.io/design/color/dark-theme.html) primary colors are desaturated for **accessibility**: saturated colors also produce optical vibrations against a dark background, which can induce eye strain. Instead, desaturated colors can be used as a more legible alternative


### Does Music Player GO uses equalizer?

Yes, Music Player GO uses the system/inbuilt (default) equalizer if present on the device.
The equalizer can be found in the now playing dialog.


### Does Music Player GO play music tracks from online sources?

No, Music Player GO doesn't play music tracks from any online source, it only plays from local library.


### Does Music Player GO access the internet?

No, Music Player GO does not access the internet to. Internet permission is not required by this app.


### How does Music Player GO categorized music tracks?

Music is categorized by artists and folders. Artists music is in turn categorized by albums. Folders can be considered albums. Each album consist of tracks of the same album.


### Where is edge-to-edge option (Lollipop, Marshmallow, Nougat or Android 8.0.0)?

This feature is not supported on these Android version. It has been added in api level 27 (Android 8.1) to avoid issues with navigation bar buttons visibility: [light navigation bar](https://developer.android.com/reference/android/R.attr#windowLightNavigationBar) is in fact available since api level 27.


### Can I exclude folders from my library?

No, the music library is scanned by the operating system. To exclude folders create a file called ".nomedia".


###What about features requests/crash reports?

1. Not all requests can be taken into consideration unfortunately. This could happen for a variety of reasons:

   - The feature is too complex clashing with the philosophy behind the app: KISS
   - The developer is kinda poor and can't afford resources like real devices that are needed for testing purposes (a dying Nexus 5 being his testing device for now) or a good computer that can ensure emulators usage. This is the case of issues [#29](https://github.com/enricocid/Music-Player-GO/issues/29) and [#51](https://github.com/enricocid/Music-Player-GO/issues/51).
   - The feature just does not match the developer's needs. Keep in mind that this app is built around the developer's usage needs and that it was shared for the love of open source.
   - The developer is a Chemist with an unstable work condition in real life, sometimes time is just short...
   - Lack of motivation/interest


2. A crash report is useful only when logcat and detailed explanation on how to reproduce the bug is provided. If You don't know how to proceed just send an email to the developer to receive guidance.


### Does this app keeps any information or data about me or my device?

**Informations or datas are not kept by this app** neither the device's nor owner's information is gotten by this app.



If you have other questions to ask which are not listed in the above FAQ, kindly ask by creating an issue. We will be available to give answers to your questions.
