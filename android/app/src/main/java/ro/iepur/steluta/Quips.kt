package ro.iepur.steluta

/**
 * Comentariile, pe fel de vreme.
 *
 * Aceleasi texte ca in varianta web - generate din ea, ca sa nu ajunga cele doua sa
 * spuna lucruri diferite despre aceeasi zi.
 */
object Quips {

    private val FURTUNA = listOf(
        "Furtună. Ăsta e momentul în care „ies doar un minut\" devine o decizie de viață.",
        "Tună. Dacă aveai planuri afară, acum ai planuri înăuntru.",
        "Cerul are o părere și ți-o spune tare. Stai în casă.",
        "Furtună. Umbrela nu te ajută, doar îți dă iluzia că faci ceva.",
        "E genul de vreme care rupe crengi și programe. Amână.",
        "Descărcări electrice afară, descărcare de baterie înăuntru. Alege înțelept.",
        "Furtună. Momentul perfect ca internetul să pice exact acum.",
        "Afară e spectacol. Bilet la geam, nu la scenă.",
        "Nu e „o ploicică\". Rămâi unde ești.",
    )

    private val NINSOARE = listOf(
        "Ninge. Arată frumos vreo douăzeci de minute, apoi devine problema ta.",
        "Zăpadă. Toată lumea conduce ca și cum ar fi prima iarnă din istorie.",
        "Ninge. Ia-ți bocancii, nu adidașii pe care-i priveai acum.",
        "E alb afară. E și alunecos, dar asta nu se vede în poze.",
        "Ninsoare. Cinci minute de magie, două ore de curățat mașina.",
        "Ninge liniștit. Bucură-te acum, mâine e gheață.",
        "Zăpadă proaspătă. Primul care calcă e erou, al doilea e doar ud.",
        "Ninge. Ceaiul nu e opțional azi.",
        "Iarna și-a adus aminte de tine.",
    )

    private val PLOAIE = listOf(
        "Plouă. Ia umbrela pe care oricum o s-o uiți undeva.",
        "Ploaie. Perfect pentru cei care nu voiau să iasă oricum.",
        "Plouă. Șosetele ude sunt la o pereche de pantofi greșiți distanță.",
        "Apă din cer. Gratis, dar nimeni n-a cerut-o.",
        "Plouă. Zi bună pentru geam, ceai și zero responsabilități.",
        "Ploaie. Mașina ta tocmai a fost spălată. Prost, dar spălată.",
        "Plouă. Gluga nu e o rușine, e o strategie.",
        "Cerul se descarcă emoțional. Lasă-l.",
        "Plouă. Dacă azi conta părul tău, ghinion.",
        "Ploaie. Alergatul se amână, iar tu știi asta deja.",
    )

    private val CEATA = listOf(
        "Ceață. Vezi cam până la capătul brațului și nici acolo cu încredere.",
        "Ceață densă. Condu ca și cum ai avea ceva de pierdut.",
        "Afară e ștearsă lumea. Revine mai târziu.",
        "Ceață. Totul arată misterios până când calci în ceva.",
        "Vizibilitate mică, răbdare și mai mică. Ai grijă pe drum.",
        "Ceață. Farurile aprinse, viteza jos, muzica mai încet.",
        "Orașul a intrat în modul incognito.",
    )

    private val VANT = listOf(
        "Vânt puternic. Coafura de azi e o sugestie, nu o decizie.",
        "Bate tare. Ține-ți gluga și demnitatea.",
        "Vânt. Umbrela ta va deveni sculptură modernă.",
        "Rafale serioase. Nu parca sub copaci.",
        "Vânt puternic. Ușile se trântesc singure, nu e casa bântuită.",
        "Afară te împinge cineva. E doar aerul.",
        "Vânt. Frigul de pe termometru și frigul real sunt două lucruri diferite azi.",
    )

    private val GER = listOf(
        "Ger. Aerul te înțeapă, nu te mângâie.",
        "E frig de crapă pietrele. Straturi, nu curaj.",
        "Ger serios. Mașina va porni, dar cu resentimente.",
        "Sub zero binișor. Nu ieși „așa, repede\", că nu ține.",
        "Frig de-ăla care te trezește mai bine decât cafeaua.",
        "Ger. Fiecare centimetru de piele descoperit e o greșeală.",
        "Iarnă adevărată, nu decor. Îmbracă-te ca atare.",
        "E atât de frig încât până și frigiderul pare o opțiune caldă.",
    )

    private val FRIG = listOf(
        "Frig. Geaca aia subțire nu e o idee bună, deși arată bine.",
        "Rece. Mâinile în buzunare, planurile scurte.",
        "E frig. Nu polar, dar suficient cât să regreți.",
        "Frig. Ceaiul e mai mult decât o băutură azi.",
        "Rece afară. Zece minute sunt suportabile, treizeci nu.",
        "Frig. Ia fularul, nu-l lăsa pe scaun.",
        "Nu e ger, dar nici primăvară. Îmbracă-te pentru varianta rea.",
    )

    private val RACOARE = listOf(
        "Răcoare. Vremea aia care nu se hotărăște ce vrea.",
        "Rece-plăcut. Un strat în plus și ești câștigător.",
        "Răcoros. Perfect de mers pe jos, prost de stat pe loc.",
        "Nici cald, nici frig. Vremea diplomatului.",
        "Răcoare. Geaca subțire își face în sfârșit treaba.",
        "E vremea în care pleci îmbrăcat gros și te întorci cărând haina.",
        "Răcoros și limpede. Se putea mult mai rău.",
    )

    private val PLACUT = listOf(
        "Vreme bună. Serios, chiar bună. Ieși puțin.",
        "Perfect afară. Nicio scuză nu ține azi.",
        "Temperatura ideală. Se întâmplă rar, folosește-o.",
        "Vreme de plimbat fără plan.",
        "Așa ar trebui să fie mereu. Nu e, deci profită.",
        "Nici cald, nici frig, nimic de reclamat. Ciudat, nu?",
        "Zi bună. Chiar și pentru cei care nu ies niciodată.",
        "Vremea nu are azi nicio scuză să te țină în casă.",
    )

    private val CALD = listOf(
        "Cald. Umbra devine brusc o resursă.",
        "E cald. Apa, nu cafeaua.",
        "Vreme de terasă, nu de birou. Îmi pare rău.",
        "Cald bine. Asfaltul o simte înaintea ta.",
        "Soare tare. Cinci minute par zece.",
        "Cald. Mașina lăsată la soare e un cuptor cu volan.",
        "Vară adevărată. Hidratare, nu eroism.",
    )

    private val CANICULA = listOf(
        "Caniculă. Nu ești obosit, ești copt.",
        "E prea cald pentru orice. Inclusiv pentru asta.",
        "Caniculă. Ieși dimineața sau seara, la mijloc nu.",
        "Peste 35. Asta nu mai e vreme, e o pedeapsă.",
        "Arșiță. Apă, umbră, zero ambiții.",
        "Caniculă. Mașina nu se conduce, se negociază.",
        "E atât de cald încât și vântul e cald. Genial.",
        "Zi de stat nemișcat lângă ceva rece.",
    )

    private val SENIN = listOf(
        "Senin complet. Nici măcar un nor de vină.",
        "Cer curat. Rar, dar se întâmplă.",
        "Fără nori. Ochelarii de soare nu sunt fițe azi.",
        "Senin. Cerul și-a făcut curat.",
        "Zi limpede. Se vede până departe și merită privit.",
        "Nicio pată pe cer. Bucură-te, mâine se schimbă.",
    )

    private val NOAPTE = listOf(
        "E noapte. Vremea de mâine e problema lui mâine.",
        "Noapte liniștită. Cerul nu are planuri cu tine.",
        "Târziu. Verifici vremea sau eviți somnul?",
        "Noapte. Dacă tot ești treaz, măcar e frumos afară.",
        "Ora la care singurul plan bun e patul.",
    )

    /**
     * Ce fel de comentariu se potriveste, in ordinea in care conteaza.
     *
     * **Vremea grea bate temperatura**: cand ploua cu galeata, pe nimeni nu-l intereseaza
     * ca sunt 18 grade placute.
     */
    private fun category(w: Weather): List<String> {
        val kind = Wmo.kindOf(w.code)
        return when {
            kind == Kind.STORM -> FURTUNA
            kind == Kind.SNOW -> NINSOARE
            kind == Kind.RAIN || kind == Kind.DRIZZLE -> PLOAIE
            kind == Kind.FOG -> CEATA
            w.wind >= 35 -> VANT
            w.temp <= -5 -> GER
            w.temp < 5 -> FRIG
            w.temp < 15 -> RACOARE
            w.temp < 24 -> if (!w.isDay) NOAPTE else if (kind == Kind.CLEAR) SENIN else PLACUT
            w.temp < 31 -> CALD
            else -> CANICULA
        }
    }

    /**
     * Alegerea nu e la intamplare de tot: se schimba din ora in ora, nu la fiecare
     * reimprospatare. Un text care sare la fiecare zece minute distrage; unul care nu se
     * schimba deloc devine invizibil in doua zile.
     */
    fun forWeather(w: Weather, nowMillis: Long = System.currentTimeMillis()): String {
        val list = category(w)
        if (list.isEmpty()) return ""
        val hours = nowMillis / 3_600_000L
        val index = ((hours * 7 + list.size) % list.size).toInt()
        return list[index]
    }
}
