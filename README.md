# How to setup Passulo

## Create a key pair

```bash
$ passulo keypair
Your keypair has been created.
The private key is stored in the file private.pem. It is used to make sure that only YOU can sign passes with your identity. Treat it like a password!
The public key is stored in the file public.pem. It allows others (the server, the app) to verify that YOU signed a pass.
```

#### Why?

The private and the public key have to be created together. Doing this on your computer means that no server EVER knows the private key. This means that no hacker can steal it to sign passes for you (as long as you keep it secure).

#### Trust

Since everything happens on your device with code that you can audit, you don't have to trust anyone or anything.

## Register public key

```bash
$ passulo register --name "My Association" --server app.passulo.com
```

#### Why?

The public key alone doesn't _mean_ anything. It has to be tied to your association, so that a server can answer: "Yes, this pass has been signed by someone from 'My Association'". Technically, anyone can write the name of your association into a pass, but only passes signed by you will receive a green checkmark from the server.

Unfortunately, this has to be a manual process. Otherwise, a hacker could register a _very similar looking_ association (see https://en.wikipedia.org/wiki/Typosquatting).

#### Trust

The human who controls the Passulo-server that you are using has to enter the connection between you and your association. Therefore,

* you have to trust them to not allow anyone else to claim that association,
* clients who read the pass have to trust them to validate any identity they add.

The app.passulo.com-server is a convenience, run by the people who built Passulo. If you don't (want to) trust them, you can run your own server-instance where you are in complete control.

## Create a Signing Certificate

```bash
$ passulo certificate --email "user@myassociation.com"
Three files have been created:
- keystore.p12
- request.certSigningRequest
- public_cert.pem
The keystore is secured with a randomly generated password that has been written to passulo.conf.

Please create an identifier at https://developer.apple.com/account/resources/identifiers/list/passTypeId (beginning with 'pass.') and create a new 'Pass Type ID Certificate' at https://developer.apple.com/account/resources/certificates/add. You need to upload the `request.certSigningRequest`.
```

#### Why?

Apple only allows registered developers to distribute Wallet Passes. Therefore they have to be signed with an Apple-issued certificate.

These two certificates have to be different, because Wallet requires RSA and Passulo requires ECC to fit the signature into the QR code.

#### Trust

Everything in this step happens locally or on the Apple Developer Portal. Since you only upload the CSR, your don't have to trust anyone.

## Import Apple Cert ?

## Download Apple WWDR CA G4 Certificate

```bash
$ passulo appleCA
The Apple WWDR CA G4 certificate has been downloaded to `AppleWWDRCAG4.cer`.
```

#### Why?

This is required to sign the pass.

#### Trust

This is a shortcut, you can also download the file directly from https://www.apple.com/certificateauthority/AppleWWDRCAG4.cer.

## Create a Template

```bash
$ passulo template
```

#### Why?

This creates a `template` folder with icons, logos and translations in the correct formats. You can replace them with your own. Make sure not to change the filenames or types (i.e. jpg instead of png).

It also creates a `members.csv` file (if it doesn't exist yet) that has the required columns prefilled.

## Create a Config

The above functions have already written some configuration into `passulo.conf`. You can change these if your files are named differently, you prefer a different color or you want to work with a different server:

```hocon
keys {
    private = private.pem
    public = public.pem
    key-identifier = "uniqueName"
    keystore = keystore.p12
    password = "randomlygeneratedpassword"
    apple-ca-cert = AppleWWDRCAG4.cer
}

pass-settings {
    association-name = "My Association"
    server = app.passulo.com
    team = "9MD9DV36EY"
    identifier = "pass.com.passulo.v1"
}

colors {
    foregroundColor = "rgb(196,50,45)"
    backgroundColor = "rgb(255,255,255)"
    labelColor = "rgb(107,107,107)"
}

app {
    associated-app = [1609117532]
}
```

## Create Passes

```bash
$ passulo create --source "members.csv"
```

This uses the files in the `template` folder and the settings from `passulo.conf` to create a pass for each entry of `members.csv`. The data is converted into a Paseto-token which is signed with the `private.pem` key and then put into the QR code. The result is then signed with the Apple-signed key from the `keystore.p12` and written to the `passes` folder. The name consist of the serial-number of the pass and the name of the member.

You can now send the passes to the members of your association.

## TODO update, delete

## Images

You can add two or three images to the pass, each in three resolutions ("@2x", "@3x"), all in `png`:

* `logo.png` is the most prominent at the front of the pass. Maximum size: 160 x 50 points (should be narrower though)
* `icon.png` is used when sharing a pass, of for notifications. Ideal size: 29 x 29 points
* `strip.png` is optional and is displayed behind the "primary fields", i.e. the number. Size: 375 x 123 points.

See https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/PassKit_PG/Creating.html for reference.



# QR Code Definitions

The content of the QR code consist of

```html
https://app.passulo.com/
    ?code=<base64 encoded token>
    &sig=<base64 encoded signature>
    &kid=<keyid>
```

The token itself is defined as protobuf in https://github.com/passulo/Token. There you also find bindings for many languages.

The signature is created over the byte-encoded token from an Ed25519 private key that belongs to the association that issues the pass.

The key-id can be used to download the public key of the issuer from the given domain. Use this public key to verify the signature.
