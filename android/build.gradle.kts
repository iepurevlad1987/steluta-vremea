// Aceleasi versiuni ca la VFit, dinadins: sunt deja descarcate pe calculatorul pe care
// se compileaza, deci primul build nu asteapta nimic de pe internet.
//
// **Kotlin nu are plugin propriu aici.** Incepand cu AGP 9, suportul Kotlin e inclus in
// plugin-ul Android, iar `org.jetbrains.kotlin.android` aplicat separat e refuzat
// explicit - exact eroarea „Failed to apply plugin 'org.jetbrains.kotlin.android'".
plugins {
    id("com.android.application") version "9.3.1" apply false
}
