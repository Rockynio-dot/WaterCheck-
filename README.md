# WaterCheck 💧

Osobní hlídač pitného režimu pro Android. Bez reklam, bez účtu, bez internetu –
všechna data zůstávají jen v telefonu.

## Co umí
- Denní cíl podle váhy, pohybu a počasí (nebo vlastní)
- Rychlý zápis nápoje (voda, čaj, káva, mléko, džus) s hydratačním faktorem
- Připomínky v době, kdy jste vzhůru, s tlačítkem pro zápis přímo z notifikace
- Historie za 7 a 30 dní, průměr a série splněných dní

## Instalace
Každý push do `main` sestaví APK. Stáhněte `WaterCheck.apk` z poslední položky
v [Releases](../../releases) a otevřete ho v telefonu.

Na Xiaomi/HyperOS po instalaci v Nastavení aplikace zapněte *Vypnout optimalizaci
baterie* a v systému povolte *Automatické spouštění*, jinak systém připomínky zabíjí.

## Vývoj
```
./gradlew testDebugUnitTest assembleRelease
```
Kotlin, Jetpack Compose, Room, DataStore, AlarmManager. Android 8+ (cílí na Android 16).

Podpisový klíč v `keystore/` je schválně v repu, aby každý build šel nainstalovat
přes předchozí bez ztráty dat. Pro osobní aplikaci to stačí; pro vydání na Google
Play by byl potřeba soukromý klíč.
