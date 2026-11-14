# SimpleToob
## Version 0.5.0

I built SimpleToob for people who want to watch YouTube without surrendering the interface to a marketing department.

It is an independent Android client with native video playback, local subscriptions, local playlists, downloads, comments, optional account integration, and enough appearance controls to stop somebody else’s design committee from choosing everything for you.

The application code is Java. Heavy work runs on separate background workers. The icon is a deliberately questionable crayon play button. Corporate branding departments can cope.

**SimpleToob works extremely well without a Google or YouTube account.** Local subscriptions, playlists, Saved videos, and history exist precisely so signing in can remain optional. They are normal features of the app, not a crippled demonstration mode.

Minimum Android version: Android 8.0 Oreo, API 26.

The official YouTube app is not required. Normal use does not require your own API key or a Google developer project.

## Who it is for

SimpleToob is for people who want:

- A useful YouTube client without an account.
- Subscriptions and playlists stored on their own device.
- Optional access to an existing YouTube account.
- Control over colors, darkness, fonts, spacing, buttons, and playback behavior.
- Downloads and readable comments.
- A conventional video interface without being dumped into a Shorts feed.
- An app built for watching, rather than another mobile upload dashboard.

Uploads are deliberately omitted.

## What it does

### Account-free watching and local collections

Without signing in, you can:

- Search for public videos.
- Open video links, supported Shorts/live links, video IDs, or pasted embed codes.
- Browse channels and public playlists.
- Play supported public video streams.
- Read public comments and replies.
- View available likes and estimated dislikes.
- Save videos locally.
- Keep local watch history and resume positions.
- Subscribe to channels locally and browse their videos.
- Create, rename, reorder, and delete local playlists.
- Play local playlist queues.
- Import a subscription CSV.
- Download supported MP4 videos.
- Use the appearance and playback settings.

Your local lists remain available when you sign out.

Locally stored lists contain references and metadata. Streaming still requires a connection. Downloading saves the actual video file.

### Native playback

Playback uses a native player with:

- Quality selection from available streams.
- Combined and separate audio/video streams.
- MP4, WebM, and supported extracted HLS playback.
- Playback-speed controls.
- Configurable seeking.
- Captions when available.
- Fullscreen playback.
- Resume checkpoints.
- Optional looping.
- Optional automatic advancement through a queue.
- Decoder and format fallbacks.
- A configurable video-rendering surface.
- Visible loading and playback errors.
- Retry that refreshes stream links.

SimpleToob does not include a YouTube ad player, an advertising SDK, or an automatic fallback to an ad-supported embedded player.

Sponsor segments baked into an uploaded video remain part of that video.

### Comments and ratings

Existing public comments load independently of whether the video plays or an account is signed in.

The native comments panel supports reading comments, replies, refreshing, and loading additional pages. Automatic loading and the maximum number of loaded comments are configurable.

With a working signed-in session, the app also provides comment submission and local draft preservation. A draft is retained when submission is not confirmed. YouTube’s moderation and visibility rules still apply.

Likes prefer available YouTube-reported metadata. Dislikes can come from Return YouTube Dislike, with an optional additional Invidious source.

Dislike figures are labeled estimates. Sources may share the same underlying data, so the app does not add their numbers together and invent a more impressive total. Marketing mathematics can stay outside.

### Optional account integration

Signing in adds account features without replacing the local library.

Implemented account paths include:

- Account verification and profile picture.
- Home, subscriptions, history, and liked-video feed access.
- Likes, dislikes, and clearing a reaction.
- Comment submission.
- Subscription actions.
- Playlist creation and adding videos.
- Importing subscriptions and playlists into the regular local collections.

The account window displays Google and YouTube’s own pages inside SimpleToob. After choosing the intended account, “Use this account” verifies the session and returns to the native app.

Being signed into Android does not automatically authorize SimpleToob. The app retains its own YouTube session.

### Subscription and playlist imports

Imports copy account collections into the same local lists used elsewhere in SimpleToob. There is no separate account-library dungeon to maintain.

Subscriptions and playlists each have independent controls for:

- Manual import.
- Import on app launch.
- Automatic intervals.
- Preset or custom intervals from 15 minutes to seven days.

Automatic importing starts disabled.

Automatic imports pause while signed out. Changing accounts or local profiles stops work associated with the previous session. Account-access rejection can also pause automatic imports until the session is recovered.

Android controls when background jobs actually execute, so intervals are approximate.

Two import behaviors are available:

- Merge keeps channels and playlists that disappear from later account results.
- Mirror removes missing previously imported collections for the selected account, while protecting local-only items and locally edited playlists.

Unedited imported playlists refresh their contents from YouTube. Locally edited copies are protected until “Allow refresh from YouTube” is explicitly selected.

Imports do not automatically upload local edits.

### Downloads

Supported MP4 sources can be downloaded to a location chosen through Android’s file picker.

Downloads provide:

- Progress reporting.
- Cancellation.
- Separate audio/video track downloading and joining.
- Continued downloading after leaving the player.
- Optional unmetered-network restrictions.

One download runs at a time. Interrupted downloads restart rather than resume. WebM and HLS downloads are not currently supported.

### Customization

Appearance controls include:

- System, light, and dark themes.
- Dark-background brightness, including black.
- Neutral, warm, and cool gray tones.
- Preset and custom RGB accent colors.
- Square or rounded buttons.
- Rounded-button radius.
- Font family and app text size.
- Panel padding.
- Standard or compact video cards.
- Channel-name and duration visibility.
- Crayon, blue-crayon, and monochrome launcher icons.

Playback and behavior controls include:

- Preferred maximum quality.
- Starting playback speed.
- Autoplay when opening a video.
- Queue advancement.
- Resume positions and history recording.
- Looping and keeping the screen awake.
- Seek-button interval.
- Video fit.
- Caption preferences, language, and size.
- Rendering surface and codec preferences.
- Decoder and format fallback.
- Startup screen.
- Home discovery topic.
- Content language and region.
- Short-video filtering.
- Hidden-channel filters.
- Comment loading and limits.
- Rating providers.
- Local, YouTube, or combined subscription actions.
- Download and import network restrictions.
- Account-import schedules and behavior.

The interface belongs on your phone. It should not require permission from a product manager to change its accent color.

## Getting started

### Install or update

Install the supplied APK on Android 8.0 or newer.

### Search and open videos

Use the search field and search button.

The link button accepts supported YouTube URLs, video IDs, and embed codes. Embed markup is parsed for its video URL; it is not executed.

You can also share a supported video link to SimpleToob through Android’s share menu.

Home uses a configurable discovery search. It does not reproduce a personalized YouTube homepage.

### Use the player

Tap the video to show or hide its controls.

Use the on-video controls to play, pause, seek, or enter fullscreen. The player-options menu contains quality, speed, captions, retry, and stream details.

The action row scrolls horizontally. Additional actions, including Download and Playlist, may be off to the side.

Leaving the player or locking the phone pauses playback.

### Save videos and use history

“Save” adds a video to your local Saved list.

Library contains Saved videos, history, and playlists. Saved supports sorting and queue playback.

History and resume behavior can be disabled in Settings.

### Add local subscriptions

Open Subs and choose Add, or subscribe from a video.

Local subscriptions need no Google account. Tap a channel in the list to browse its videos.

Holding a subscription offers local removal. This does not unsubscribe on YouTube. If account imports are enabled and you remain subscribed there, a later import can restore the local entry.

### Import a subscription CSV

Use CSV in Subs and select an extracted Google Takeout subscriptions.csv file.

Extract the CSV from the Takeout ZIP first. The app imports the CSV itself.

### Create and manage playlists

Open Playlists through Library or Account.

“Create playlist” creates a local playlist. When signed in, it also offers creation on YouTube.

Use the player’s Playlist action to add a video. Matching imported playlists can offer either a local-only addition or an explicit addition to YouTube and the local copy.

Hold a playlist to rename or delete it. Imported playlists also provide “Allow refresh from YouTube.”

Inside a local playlist, hold a video for move-up, move-down, or removal options.

“Play queue” starts playlist playback. Automatic advancement is controlled separately in Settings.

### Read or write comments

Tap the Comments preview beneath a video.

The panel provides comments, replies, refresh, additional pages, and composition. Existing public comments work without signing in.

Posting requires a working account session. Drafts remain local until submission is confirmed.

### Download a video

Choose a supported MP4 quality, then use Download.

Select the destination through Android’s file picker. Notification permission allows visible download progress and cancellation controls where Android supports them.

### Sign in, if you want to

Open the account icon and choose sign-in.

Complete account selection inside the in-app account window. If YouTube is already signed into the intended account there, use “Use this account.”

Verification errors appear in a dismissible OK dialog. Successful verification returns to the native interface.

The account-window menu includes account selection, text size, rendering mode, display information, and session removal.

### Configure account imports

Open “Import & schedule” from Subs, Playlists, or Account.

The same controls are available under:

Settings → Profiles and library → Account imports

Choose which collection to import manually, enable launch-time imports if wanted, and configure each interval independently.

Review Merge versus Mirror before enabling automatic refresh. Local edits are not automatically uploaded.

### Change appearance and behavior

Open Settings and select the relevant category.

Appearance controls colors and themes. Interface and controls contains fonts, spacing, and button shape. Playback and Playback engine contain viewing preferences and rendering controls.

Profiles and library manages local profiles and stored collections.

## Storage and privacy

Saved videos, history, subscriptions, and playlists are kept in app-private local storage, separated by local profile.

Saved native YouTube session snapshots are encrypted using Android Keystore. The account window also retains its own app-private browser cookies and page storage to preserve sign-in progress.

Removing the app session clears its saved session and account-window cookies. Local collections remain available.

Android backup is disabled. Do not assume app-private data will survive uninstalling the app.

I have not added a SimpleToob analytics SDK. YouTube still receives the requests needed to provide its content. Enabled rating providers receive video IDs, and avatar hosts receive image requests.

Diagnostics exclude passwords, session cookies, comment contents, and full stream URLs.

## Beta limits and troubleshooting

YouTube changes its website and request formats. That can break extraction or account features and require a client update.

Current limits include:

- No uploads.
- No complete desktop/mobile YouTube feature parity.
- No background or screen-off playback, picture-in-picture, casting, or floating miniplayer.
- No automatic bidirectional account synchronization.
- No automatic access to private, paid, DRM-protected, or age-restricted video playback merely because an account is signed in.
- Some account feeds currently load an initial page rather than a complete archive.
- Dynamic mixes and unavailable playlist entries may fail or be omitted.
- Local playlists store one entry per video ID, so repeated occurrences collapse.
- Import safety limits of 500 continuation pages and 50,000 playable entries per playlist.
- No resumable downloads or WebM/HLS downloading.

## Complete changelog

### 0.5.0 — Shared collections and account imports

Added:

- Signed-in subscription imports into the normal local subscription list.
- Signed-in playlist imports into the normal local playlist collection.
- Importing playlist contents across continuation pages.
- Independent manual import controls for subscriptions and playlists.
- Independent import-on-launch settings.
- Independent preset and custom background intervals.
- Persistent Android background scheduling.
- Optional unmetered-only importing.
- Import progress, completion information, and cancellation.
- Merge and Mirror import behavior.
- Per-account import ownership.
- Protection for locally edited imported playlists.
- Explicit permission to replace local edits on a later refresh.
- Account profile pictures in the toolbar and account screen.
- Background account-image refresh.
- Public YouTube/Google avatar-host support.
- A separate import coordinator and three bounded import-read workers.
- Database schema 4 and migration support.
- Collection-parser, identity, scheduling-policy, and SQLite preservation checks.
- An Android database-import regression test.

Changed and fixed:

- Replaced isolated account subscription and playlist entry points with shared local collections.
- Added support for current playlist lockup layouts.
- Fixed signed-in playlist pagination.
- Matched imports to the selected account and channel.
- Bound requests and commits to a profile and session revision.
- Cancelled old-session work when signing out or switching accounts/profiles.
- Paused automatic imports after account-access rejection.
- Separated attempt times from successful completion times.
- Limited interval retries after failures.
- Preserved previous local playlist copies when a refresh fails.
- Allowed other playlist imports to complete when an individual playlist fails.
- Prevented unrecognized empty layouts from authorizing deletion.
- Protected local-only data and other owners’ imports during Mirror cleanup.
- Added recycled subscription and playlist index rows.
- Loaded imported local playlist queues by database ID instead of oversized Intent payloads.
- Kept explicit local-only and YouTube-and-local playlist additions in one selection flow.
- Preserved existing local data during the database upgrade.

### 0.4.3 — Account verification and useful error messages

Changed and fixed:

- Replaced the old account-menu verification request with the current account-list request.
- Corrected the channel-switcher request fields.
- Matched the web context and client-name/version headers.
- Separated ordinary website page requests from authenticated API POST requests.
- Preserved the selected Google account slot and optional channel delegation.
- Captured cookies from the active YouTube host.
- Kept older encrypted cookie-only records readable.
- Corrected verification that returned HTTP 400 while the embedded website was already signed in.
- Added distinct diagnostics for website HTTP errors and native verification errors.
- Added protocol, account-selection, and record-migration tests.

Removed and simplified:

- Removed the persistent session blurb and inline error footer.
- Replaced verification failures with dismissible OK dialogs.
- Renamed the account-window action to “Use this account.”
- Returned to the native app after successful verification.
- Added a brief success toast.

### 0.4.2 — Account-page rendering compatibility

Added and changed:

- Defaulted the account WebView to software compatibility rendering.
- Kept native video playback accelerated.
- Added a live hardware/software rendering selector.
- Preserved the page and cookies while changing rendering mode.
- Added display information showing the WebView version, drawing mode, dimensions, density, and text zoom.
- Added copying of sanitized display diagnostics.
- Extended offline viewport test coverage to both rendering modes.

This was a targeted workaround for distorted form text, not proof that every WebView rendering problem was resolved.

### 0.4.1 — Sign-in loops, session persistence, and page sizing

Changed and fixed:

- Removed automatic sign-in reloads after YouTube landing pages.
- Allowed authenticated landing pages to finish loading.
- Preserved browser cookies and page storage for the same local profile.
- Restored account-page state across Activity recreation.
- Cleared browser state when switching browser profiles or explicitly removing the session.
- Enabled mobile viewport metadata.
- Disabled overview-mode shrinking.
- Added pinch zoom and account-page text-size controls.
- Added the Google account chooser.
- Enabled account-window cross-site cookies.
- Allowed scoped Google consent and cookie-handoff routes.
- Added a bounded redirect-loop guard.
- Required an explicit retry after a stopped loop.
- Handled account-renderer loss with a visible error rather than an automatic reload.
- Added navigation diagnostics and login-flow regression tests.

### 0.4.0 — Major interface rework

Added:

- Icon-based bottom navigation.
- Compact search controls.
- Horizontally scrolling topic chips.
- Standard 16:9 thumbnail cards.
- Duration badges.
- Horizontal compact cards.
- Per-video options controls.
- Playback controls over the video.
- Automatic control hiding while playing.
- A player-options menu.
- A native comments panel and watch-page preview.
- Categorized settings.
- Cleaner library, playlist, account, and sign-in screens.
- Explicit signed-out, saved-but-unverified, and verified account states.
- Account-routing regression tests.

Fixed:

- Corrected the account entry URL that opened a public channel named “account.”
- Normalized old account/channel entry links.
- Stopped treating page titles as account identity.
- Cleared old verification when session cookies were replaced.
- Made playback retry start the refreshed stream.
- Prevented older rating callbacks from replacing newer results.

Removed from the main layout:

- Large solid bottom-tab blocks.
- The fixed browsing footer.
- Separate playback transport rows beneath the video.
- Always-visible codec and stream details.
- The oversized multi-row action grid.
- Nested inline comment scrolling.

### 0.3.0 — Native web sessions and expanded functionality

Added:

- In-app Google/YouTube account pages.
- Encrypted per-profile native session storage.
- Native request paths for account verification, feeds, reactions, subscription actions, comments, and playlists.
- Native comment composition and saved drafts.
- Local playlist creation, renaming, reordering, deletion, and queues.
- Public playlist and channel search.
- Account playlist listing, creation, and video addition paths.
- Related-video access.
- Return YouTube Dislike estimates.
- Optional Invidious rating data.
- Source labels, timestamps, caching, and rate-limit backoff for ratings.
- Separate bounded pools for network, media, account, rating, image, and database work.
- Worker concurrency and cancellation checks.
- Bounded playback diagnostics.
- An offline H.264/AAC player test.
- Additional parser, protocol, rating, and media-selection checks.

Playback changes:

- Defaulted to SurfaceView.
- Retained TextureView as an option.
- Added an AVC preference and configurable codec selection.
- Added decoder fallback.
- Added alternative-stream fallback.
- Expanded playback to WebM and extracted HLS.
- Added visible loading, first-frame timeout, and playback errors.
- Refreshed expired stream links on retry.
- Matched supported subtitle formats to the player.
- Added configurable fit, loop, autoplay, keep-awake, seeking, captions, and starting-speed behavior.

Interface and behavior changes:

- Loaded public comments independently of media playback and login.
- Recycled comment rows and limited loaded comments.
- Added font-family, font-size, spacing, and button-radius controls.
- Added card-detail visibility settings.
- Added content language, region, and Home-topic controls.
- Added configurable subscription destinations.
- Added rating-provider settings and network restrictions.

Removed:

- External browser handoffs for web, account, and comment actions.
- Automatic fallback to an embedded ad-supported player.

### 0.2.1 — Startup crash fix

Fixed:

- The startup crash caused by requesting window controls before the window decor existed.
- Changed startup ordering so content was installed before decor-dependent work.
- Deferred system-bar appearance changes until the root view was attached.
- Added safe handling for a missing window controller.
- Preserved the existing inset handling.

Added:

- A cold-launch and Activity-recreation regression test.

### 0.2.0 — Native playback and a useful local library

Added:

- Native Media3 playback.
- Public website extraction through NewPipeExtractor.
- Playback of public MP4 sources, including separate video/audio tracks.
- Native quality selection, seeking, speed, fullscreen, and supported captions.
- MP4 downloads through Android’s file picker.
- Separate-track downloading and muxing.
- Foreground download progress and cancellation.
- Download continuation after leaving the player.
- Cleanup after handled download failures and cancellation.
- Local profiles with separate Saved videos, history, and subscriptions.
- Preservation of the original Default-profile library.
- Local channel subscriptions and channel browsing.
- Subscription removal.
- Google Takeout subscription CSV import.
- Native public comments, replies, and pagination.
- Additional dark-gray, tone, accent, button-shape, compact-card, and launcher-icon controls.

Changed and fixed:

- Replaced API-key-dependent public data access with website extraction.
- Reworked the interface around Home, Search, Subs, Library, and Settings.
- Moved Saved and history into Library.
- Reworked window sizing and system/keyboard insets.
- Addressed unused lower-screen space on the Razr.
- Used current window bounds rather than fixed device-DPI assumptions.
- Added handling for rotation, folding, keyboard changes, and split-window sizing.
- Kept extraction, images, storage, and downloads off the UI thread.

Removed:

- The earlier iframe playback path.
- Personal API-key setup and stored key preferences.
- Terms of Service and privacy-policy entries from Settings.

At this stage, account actions still used a browser handoff. That was replaced in 0.3.0.

Uploads remained deliberately omitted.

### 0.1.0 — Initial implementation

Added:

- The initial Java Android project.
- Basic navigation and search.
- API-backed public data access.
- Official embedded playback.
- Local Saved videos and history.
- Startup-screen selection.
- Saved-list sorting and queue groundwork.
- Appearance controls.
- Local channel filtering and short-video filtering groundwork.
- The crayon-style launcher identity.
- Initial build configuration and Windows build instructions.

Research and planning:

- Reviewed recurring complaints from Reddit, forums, and public Play Store reviews.
- Covered advertising disruption, forced Shorts behavior, navigation changes, playback stutter/resets, obstructive miniplayer/fullscreen controls, quality selection, speed resets, irrelevant search, Saved/playlist sorting, and comments.
- Used those reports as development priorities, without pretending a qualitative sample was a scientifically ranked worldwide top ten.

### Project-wide work

Throughout development I have:

- Kept application source in Java.
- Separated expensive work from Android’s UI thread.
- Added bounded queues and rejected-work handling.
- Preserved the development signing identity for in-place updates.
- Maintained a reproducible SDK-based APK build alongside the Gradle project.
- Added source archives, dependency provenance, checksums, and required notices.
- Added parser, transport, account, concurrency, scheduling, and migration checks.
- Kept uploaded personal logs and credential-page screenshots out of distributed project archives.
- Documented implemented behavior and remaining limits instead of calling every successful compile a finished feature.

## Licensing

I offer my original, independently distributed work under my No Faults Software License:

https://github.com/ZoKLav/no_fault_license

The current NewPipe-based combined application is distributed under GPL-3.0-or-later. Third-party components retain their own licenses, and the bundled notices apply.
