#!/bin/zsh
set -e
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="/Users/kingwamble/Library/Android/sdk"
/Users/kingwamble/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle -p /Users/kingwamble/.gemini/antigravity/scratch/OneUIStocks -Dorg.gradle.java.home="/Applications/Android Studio.app/Contents/jbr/Contents/Home" assembleDebug
