/*
 * Copyright [2023-2025] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.lib;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.Resources;
import java.util.ArrayList;
import java.util.Locale;

@SuppressWarnings("Convert2Lambda")
public class libLanguage {
    /*
        How to use:

        1. Define if library auto-finds the language or not (when not define the language code yourself look at: https://www.science.co.il/language/Locale-codes.php line 'Language Code' in the table)
        2. Set Language Files folder
        3. Enter translation key z.b hello.world

        INFO: All files must be located in the resources folder


        Language skeleton (Name the file as the language code it has z.b de.json)


        {
           "hello.world" : "Hello World",
           "hello.world2" : "Hello World 2"
         }
         */
    @SuppressWarnings("NonAsciiCharacters")
    public enum Language {
        ABKHAZIAN("Abkhazian", "ab"),
        AFAR("Afar", "aa"),
        AFRIKAANS("Afrikaans", "af"),
        AKAN("Akan", "ak"),
        ALBANIAN("Albanian", "sq"),
        AMHARIC("Amharic", "am"),
        ARABIC("Arabic", "ar"),
        ARAGONESE("Aragonese", "an"),
        ARMENIAN("Armenian", "hy"),
        ASSAMESE("Assamese", "as"),
        AVARIC("Avaric", "av"),
        AVESTAN("Avestan", "ae"),
        AYMARA("Aymara", "ay"),
        AZERBAIJANI("Azerbaijani", "az"),
        BAMBARA("Bambara", "bm"),
        BASHKIR("Bashkir", "ba"),
        BASQUE("Basque", "eu"),
        BELARUSIAN("Belarusian", "be"),
        BENGALI("Bengali", "bn"),
        BISLAMA("Bislama", "bi"),
        BOSNIAN("Bosnian", "bs"),
        BRETON("Breton", "br"),
        BULGARIAN("Bulgarian", "bg"),
        BURMESE("Burmese", "my"),
        CATALAN("Catalan", "ca"),
        CHAMORRO("Chamorro", "ch"),
        CHECHEN("Chechen", "ce"),
        CHICHEWA("Chichewa", "ny"),
        CHINESE("Chinese", "zh"),
        CHURCH_SLAVONIC("Church Slavonic", "cu"),
        CHUVASH("Chuvash", "cv"),
        CORNISH("Cornish", "kw"),
        CORSICAN("Corsican", "co"),
        CREE("Cree", "cr"),
        CROATIAN("Croatian", "hr"),
        CZECH("Czech", "cs"),
        DANISH("Danish", "da"),
        DIVEHI("Divehi", "dv"),
        DUTCH("Dutch", "nl"),
        DZONGKHA("Dzongkha", "dz"),
        ENGLISH("English", "en"),
        ESPERANTO("Esperanto", "eo"),
        ESTONIAN("Estonian", "et"),
        EWE("Ewe", "ee"),
        FAROESE("Faroese", "fo"),
        FIJIAN("Fijian", "fj"),
        FINNISH("Finnish", "fi"),
        FRENCH("French", "fr"),
        WESTERN_FRISIAN("Western Frisian", "fy"),
        FULAH("Fulah", "ff"),
        GAELIC("Gaelic", "gd"),
        GALICIAN("Galician", "gl"),
        GANDA("Ganda", "lg"),
        GEORGIAN("Georgian", "ka"),
        GERMAN("German", "de"),
        GREEK("Greek", "el"),
        KALAALLISUT("Kalaallisut", "kl"),
        GUARANI("Guarani", "gn"),
        GUJARATI("Gujarati", "gu"),
        HAITIAN("Haitian", "ht"),
        HAUSA("Hausa", "ha"),
        HEBREW("Hebrew", "he"),
        HERERO("Herero", "hz"),
        HINDI("Hindi", "hi"),
        HIRI_MOTU("Hiri Motu", "ho"),
        HUNGARIAN("Hungarian", "hu"),
        ICELANDIC("Icelandic", "is"),
        IDO("Ido", "io"),
        IGBO("Igbo", "ig"),
        INDONESIAN("Indonesian", "id"),
        INTERLINGUE("Interlingue", "ie"),
        INUKTITUT("Inuktitut", "iu"),
        INUPIAQ("Inupiaq", "ik"),
        IRISH("Irish", "ga"),
        ITALIAN("Italian", "it"),
        JAPANESE("Japanese", "ja"),
        JAVANESE("Javanese", "jv"),
        KANNADA("Kannada", "kn"),
        KANURI("Kanuri", "kr"),
        KASHMIRI("Kashmiri", "ks"),
        KAZAKH("Kazakh", "kk"),
        CENTRAL_KHMER("Central Khmer", "km"),
        KIKUYU("Kikuyu", "ki"),
        KINYARWANDA("Kinyarwanda", "rw"),
        KIRGHIZ("Kirghiz", "ky"),
        KOMI("Komi", "kv"),
        KONGO("Kongo", "kg"),
        KOREAN("Korean", "ko"),
        KUANYAMA("Kuanyama", "kj"),
        KURDISH("Kurdish", "ku"),
        LAO("Lao", "lo"),
        LATIN("Latin", "la"),
        LATVIAN("Latvian", "lv"),
        LIMBURGAN("Limburgan", "li"),
        LINGALA("Lingala", "ln"),
        LITHUANIAN("Lithuanian", "lt"),
        LUBA_KATANGA("Luba_Katanga", "lu"),
        LUXEMBOURGISH("Luxembourgish", "lb"),
        MACEDONIAN("Macedonian", "mk"),
        MALAGASY("Malagasy", "mg"),
        MALAY("Malay", "ms"),
        MALAYALAM("Malayalam", "ml"),
        MALTESE("Maltese", "mt"),
        MANX("Manx", "gv"),
        MAORI("Maori", "mi"),
        MARATHI("Marathi", "mr"),
        MARSHALLESE("Marshallese", "mh"),
        MONGOLIAN("Mongolian", "mn"),
        NAURU("Nauru", "na"),
        NAVAJO("Navajo", "nv"),
        NORTH_NDEBELE("North Ndebele", "nd"),
        SOUTH_NDEBELE("South Ndebele", "nr"),
        NDONGA("Ndonga", "ng"),
        NEPALI("Nepali", "ne"),
        NORWEGIAN("Norwegian", "no"),
        NORWEGIAN_BOKMÅL("Norwegian Bokmål", "nb"),
        NORWEGIAN_NYNORSK("Norwegian Nynorsk", "nn"),
        SICHUAN_YI("Sichuan Yi", "ii"),
        OCCITAN("Occitan", "oc"),
        OJIBWA("Ojibwa", "oj"),
        ORIYA("Oriya", "or"),
        OROMO("Oromo", "om"),
        OSSETIAN("Ossetian", "os"),
        PALI("Pali", "pi"),
        PASHTO("Pashto", "ps"),
        PERSIAN("Persian", "fa"),
        POLISH("Polish", "pl"),
        PORTUGUESE("Portuguese", "pt"),
        PUNJABI("Punjabi", "pa"),
        QUECHUA("Quechua", "qu"),
        ROMANIAN("Romanian", "ro"),
        ROMANSH("Romansh", "rm"),
        RUNDI("Rundi", "rn"),
        RUSSIAN("Russian", "ru"),
        NORTHERN_SAMI("Northern Sami", "se"),
        SAMOAN("Samoan", "sm"),
        SANGO("Sango", "sg"),
        SANSKRIT("Sanskrit", "sa"),
        SARDINIAN("Sardinian", "sc"),
        SERBIAN("Serbian", "sr"),
        SHONA("Shona", "sn"),
        SINDHI("Sindhi", "sd"),
        SINHALA("Sinhala", "si"),
        SLOVAK("Slovak", "sk"),
        SLOVENIAN("Slovenian", "sl"),
        SOMALI("Somali", "so"),
        SOUTHERN_SOTHO("Southern Sotho", "st"),
        SPANISH("Spanish", "es"),
        SUNDANESE("Sundanese", "su"),
        SWAHILI("Swahili", "sw"),
        SWATI("Swati", "ss"),
        SWEDISH("Swedish", "sv"),
        TAGALOG("Tagalog", "tl"),
        TAHITIAN("Tahitian", "ty"),
        TAJIK("Tajik", "tg"),
        TAMIL("Tamil", "ta"),
        TATAR("Tatar", "tt"),
        TELUGU("Telugu", "te"),
        THAI("Thai", "th"),
        TIBETAN("Tibetan", "bo"),
        TIGRINYA("Tigrinya", "ti"),
        TONGA_TONGA_ISLANDS("Tonga_Tonga_Islands", "to"),
        TSONGA("Tsonga", "ts"),
        TSWANA("Tswana", "tn"),
        TURKISH("Turkish", "tr"),
        TURKMEN("Turkmen", "tk"),
        TWI("Twi", "tw"),
        UIGHUR("Uighur", "ug"),
        UKRAINIAN("Ukrainian", "uk"),
        URDU("Urdu", "ur"),
        UZBEK("Uzbek", "uz"),
        VENDA("Venda", "ve"),
        VIETNAMESE("Vietnamese", "vi"),
        VOLAPÜK("Volapük", "vo"),
        WALLOON("Walloon", "wa"),
        WELSH("Welsh", "cy"),
        WOLOF("Wolof", "wo"),
        XHOSA("Xhosa", "xh"),
        YIDDISH("Yiddish", "yi"),
        YORUBA("Yoruba", "yo"),
        ZHUANG("Zhuang", "za"),
        ZULU("Zulu", "zu");

        final String sc;
        final String sn;

        Language(String name, String code) {
            sc = code;
            sn = name;
        }

        public String getName() {
            return sn;
        }

        public String getCode() {
            return sc;
        }

        public static String getNameFromCode(String code) {
            for (Language e : Language.values()) {
                if (e.sc.equalsIgnoreCase(code)) {
                    return e.sn;
                }
            }
            return "";
        }

        public static String getCodeFromName(String name) {
            for (Language e : Language.values()) {
                if (e.sn.equalsIgnoreCase(name)) {
                    return e.sc;
                }
            }
            return "";
        }
    }

    private final Class<?> clazz;

    public libLanguage(Class<?> clazz) {
        this.clazz = clazz;
    }

    boolean afl = false;
    String languageCode = System.getProperty("user.language");
    String lf = "";

    public Locale getLocale() {
        return Locale.forLanguageTag(Language.getNameFromCode(languageCode));
    }

    public void setNoAutoFindLanguage(String code) {
        afl = false;
        languageCode = code;
    }

    boolean langNotFound = false;

    public void setAutoFindLanguage() {
        afl = true;
    }

    public void setLanguageFolder(String path) {
        lf = path;
    }

    public String removeComment(String json) {
        StringBuilder builder = new StringBuilder();
        for (String s : json.split("\n")) {
            if (s.replaceAll(" ", "").startsWith("//")) {
                continue;
            }
            builder.append(s);
        }
        return builder.toString();
    }

    JsonObject jsoncache = null;

    public String translateHTML(String htmlfile) {
        StringBuilder cache = new StringBuilder();
        for (String s : htmlfile.split("\n")) {
            if (s.equalsIgnoreCase("")) {
                continue; //Don't waste time here
            }
            if (s.contains("(TRANSLATE)")) {
                s = s.replace(s.split("\\(TRANSLATE\\)")[1].replace("(TRANSLATE)", ""), PublicValues.language.translate(s.split("\\(TRANSLATE\\)")[1].replace("(TRANSLATE)", "")));
                s = s.replace("(TRANSLATE)", "");
            }
            cache.append(s);
        }
        return cache.toString();
    }

    public String translate(String key) {
        final String[] ret = {key};
        if(jsoncache == null) {
            try {
                jsoncache = new Gson().fromJson(removeComment(new Resources().readToString(clazz, lf + "/" + languageCode + ".json")), JsonObject.class);
            } catch (Exception e) {
                // Can be too early in init. So no ConsoleLogging.Throwable
                ConsoleLogging.error("Failed to get translation for: " + key);
                e.printStackTrace();
                return key;
            }
        }
        try {
            if(!jsoncache.has(key)) {
                ConsoleLogging.error("Failed to get translation for: " + key);
                return key;
            }
            ret[0] = jsoncache.get(key).getAsString();
        } catch (Exception e) {
            // Can be too early in init. So no ConsoleLogging.Throwable
            e.printStackTrace();
        }
        return ret[0].replaceAll("%APPNAME%", ApplicationUtils.getName());
    }

    public ArrayList<String> getAvailableLanguages() {
        ArrayList<String> languages = new ArrayList<>();
        for (libLanguage.Language language : libLanguage.Language.values()) {
            if (languages.contains(language.getName())) {
                continue;
            }
            if (new Resources().readToInputStream(clazz, "lang/" + language.getCode() + ".json") != null) {
                languages.add(language.getName());
            }
        }
        return languages;
    }
}
