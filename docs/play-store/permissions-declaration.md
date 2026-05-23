# HeaterRC — Google Play Permissions Declaration

> **Status: STALE — DO NOT USE.**
>
> The scheduled-SMS feature (receive-side SMS handling, boot-persisted recurring alarms, prepaid tracking) was removed from the codebase in the Phase-2 cleanup on 2026-05-21. The current APK is send-only and does not need the RECEIVE_SMS / RECEIVE_BOOT_COMPLETED justifications described below.
>
> This document will be rewritten when the scheduled-SMS feature is rebuilt cleanly after the Compose migration. Until then, treat its contents as a description of the *historical* feature for context, not a submission draft.

> **Status (original draft):** Draft for submission via Play Console → *Policy → App content → Sensitive app permissions → Declare permissions*.
> Submission language: English (Google Play reviewers).
> Last updated: 2026-05-20.

> ## ⚠ Pre-submission note — DO NOT SUBMIT AS-IS
>
> Static audit of the version-2.2 source tree found that the receiver classes are dead code:
>
> | Class           | State                          | Effect                                                                |
> | --------------- | ------------------------------ | --------------------------------------------------------------------- |
> | `SMSreceiver`   | `onReceive` body commented out | App currently **does not consume** any incoming SMS — `RECEIVE_SMS` is not actually exercised. |
> | `AlarmReceiver` | `onReceive` body commented out | Scheduled `AlarmManager.set` fires but does nothing.                  |
> | `BootReceiver`  | `onReceive` body commented out | Reboot persistence non-functional — `RECEIVE_BOOT_COMPLETED` not needed. |
> | `myApp.setRecurringAlarm` | body commented out   | Recurring weekly schedule non-functional.                             |
>
> **Consequence for the declaration:** the *Compliance with policy criteria* section below describes the intended (and historically working) status-feedback feature. If the next Play Store release ships **without** reactivating `SMSreceiver` and `AlarmReceiver`, the submitted APK will not match this declaration — reviewers may install the app, see that incoming SMS isn't processed, and reject as misleading.
>
> **Before submitting:**
> 1. Either reactivate the receivers (Phase 1/2 of modernization) and ship them in the same release, OR
> 2. Re-scope this declaration to *send-only* — request only `SEND_SMS` and rewrite the receive-side justification out.
>
> The send-only path loses the "status feedback from heater" argument but is the smaller, lower-risk change.

---

## App information

| Field            | Value                                |
| ---------------- | ------------------------------------ |
| App name         | HeaterRC                             |
| Package          | `dev.expeditioneer.preheat`          |
| Current version  | 2.2 (versionCode 22)                 |
| Permissions declared | `SEND_SMS`, `RECEIVE_SMS`, `RECEIVE_BOOT_COMPLETED` |
| Default handler? | No — companion-app exception claimed |

---

## Selected exception category

**Connected device companion apps** — automotive subcategory.

Per Play Console policy *Use of SMS or Call Log permission groups*, "Connected device companion apps" is an enumerated permitted exception for companion apps to smartwatches, automotive, and smart home devices. HeaterRC is the companion app for aftermarket and OEM **automotive auxiliary (parking) heaters** such as Webasto *Thermo Top* and Eberspächer *Hydronic* series. These devices are factory-fitted or retrofitted into passenger and commercial vehicles and ship with a GSM telematics module (e.g. Webasto *ThermoCall*, Eberspächer *EasyStart Call*) whose published, OEM-documented control protocol is SMS.

---

## Core functionality

HeaterRC has a **single** core function: remote control of an automotive auxiliary heater via SMS, sent to and received from one user-configured phone number (the SIM card inside the heater's GSM module).

The Play Store listing, app icon, screenshots, and in-app help text all describe this single purpose exclusively. There are no secondary features that would broaden the app's scope.

**Why SMS specifically:**
- The heater hardware exposes no other control interface — no Bluetooth, no Wi-Fi, no IP/REST API.
- The SMS command grammar is fixed by the heater OEM and documented in the heater's user manual.
- Without `SEND_SMS`, no control command can be delivered → the app is non-functional.
- Without `RECEIVE_SMS`, the user cannot see whether the heater accepted the command or what the resulting cabin temperature is → the app cannot fulfil its purpose.

There is **no alternative API, web service, or third-party data source** that can substitute for these permissions.

---

## Compliance with policy criteria

### 1. Permission usage is essential and integral
Removing either SMS permission renders the app entirely non-functional. SMS is dictated by the heater OEM and is the only supported control channel.

### 2. Prominently documented
The Play Store description's first sentence states that the app sends and receives SMS to control an automotive auxiliary heater. The same is stated on the in-app *Help* screen and in the privacy policy.

### 3. User-initiated, fixed single recipient
- All outgoing SMS are addressed **only** to the single phone number that the user manually enters in the Config screen.
- Outgoing SMS are triggered **exclusively** by direct user action (tapping a labelled command button) or by a single user-set one-shot timer.
- The app **never** auto-sends, bulk-sends, sends to multiple recipients, or accesses the user's address book / contacts.

### 4. Data minimisation and isolation
- The app does **not** read the user's existing SMS inbox or message history.
- Incoming SMS are inspected only when the sender number equals the configured heater number; SMS from any other sender are ignored and remain in the user's default SMS app inbox untouched.
- No SMS content is uploaded to any server, transmitted to third parties, used for advertising, or used for analytics.
- No alternative permission (e.g. `READ_SMS`, `READ_CALL_LOG`) is used to derive SMS-related data.
- The configured heater phone number is stored **locally** on the device only.

---

## Other declared permissions

| Permission                | Purpose                                                                                                                                          |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `RECEIVE_BOOT_COMPLETED`  | Re-registers the user's scheduled one-shot timer with `AlarmManager` (or equivalent) after a device reboot, so a scheduled heater start is not lost. No SMS is sent at boot itself. |

---

## Privacy policy clause (template — must be hosted at a public URL before submission)

```
SMS permissions

HeaterRC uses the Android SEND_SMS and RECEIVE_SMS permissions solely to communicate
with the user-configured automotive auxiliary heater GSM module. Specifically:

- SEND_SMS is used to transmit heater control commands (start, stop, set temperature,
  set duration) to the single phone number that the user entered in the app's
  configuration screen. SMS is the only control interface supported by the heater
  hardware.
- RECEIVE_SMS is used to receive status replies (acknowledgement, error code, cabin
  temperature) from that same configured number. Incoming SMS from any other sender
  is ignored by HeaterRC and remains in the user's default SMS app inbox unchanged.

HeaterRC does not read, store, transmit, or share any SMS content from senders other
than the configured heater number. SMS data is never uploaded to any server, used for
advertising or analytics, or shared with third parties. The configured heater phone
number is stored only on the user's device.

HeaterRC uses the RECEIVE_BOOT_COMPLETED permission solely to restore the user's
single scheduled heater-start timer after a device reboot, so the scheduled time is
not lost if the device restarts before the timer fires.
```

---

## Play Store listing — short description (suggested wording)

> Remote control for your automotive auxiliary parking heater (Webasto / Eberspächer
> and compatible GSM-equipped heaters) via SMS. Send start, stop and timed commands
> to your heater's GSM module from anywhere.

(The phrase *"via SMS"* must remain in the short description — it is the policy
hook that connects the listing to the declared use case.)

---

## Submission checklist

- [ ] Privacy policy published at a stable public URL (e.g. GitHub Pages) and linked in Play Console.
- [ ] Short description on Play Store explicitly mentions "via SMS" and "automotive auxiliary heater".
- [ ] App's in-app *Help / Info* screen visible to the reviewer without requiring an account.
- [ ] Screenshots show: command buttons (Home tab), phone-number config field (Config tab), incoming-status notification.
- [ ] APK/AAB compiled against the policy-current `targetSdk`.
- [ ] Permissions Declaration form: select "Connected device companion apps" → free-text justification: paste the *Core functionality* + *Compliance with policy criteria* sections above.
- [ ] If asked for a demonstration video: short screencap showing user entering a heater number, tapping *Start*, receiving an SMS-reply notification.

---

## Fallback if rejected

If Google Play rejects this declaration, the options are, in order of decreasing user reach:

1. Re-submit with stronger evidence (link to Webasto/Eberspächer OEM manual pages showing SMS as the documented control protocol).
2. Distribute via **F-Droid** and/or direct APK on the project website. No Play Store policy applies; full SMS functionality retained.
3. Pivot to hardware with a non-SMS interface (newer Webasto *ThermoConnect* uses Wi-Fi/4G + REST API). Major scope change.
4. Discontinue the app.
