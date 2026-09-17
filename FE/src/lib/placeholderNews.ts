export type PlaceholderNewsItem = {
  title: string;
  url: string;
  image: string;
};

export type PlaceholderNewsRow = {
  source: string;
  items: PlaceholderNewsItem[];
};

// Static stand-in for the V2 News Aggregator, which will fetch this from
// our own backend instead of hardcoding it here. Real headlines/links/images
// captured 2026-09-16, used only to preview the intended layout.
export const PLACEHOLDER_NEWS_ROWS: PlaceholderNewsRow[] = [
  {
    source: "N1",
    items: [
      {
        title: 'Вучић: Не треба да буде еуфорије због пресуде челницима ОВК, моје процене су биле "двоструке"',
        url: "https://n1info.rs/vesti/vucic-ne-treba-da-bude-euforije-zbog-presude-celnicima-ovk-ustavni-sud-kosova-moze-da-vrati-postupak/",
        image: "/images/news/n1-1.webp",
      },
      {
        title: "(BLOG) Вишегодишње казне за Тачија, Краснићија, Весељија и Сељимија: Немири у Приштини и Хагу",
        url: "https://n1info.rs/vesti/blog-presuda-hasim-taci-kadri-veselji-hag-ovk-uck-16092026/",
        image: "/images/news/n1-2.webp",
      },
      {
        title:
          "Спасојевић: Не очекујем уобичајено изборно вече, неће бити спора око резултата, већ око тога колико су избори били фер",
        url: "https://n1info.rs/vesti/spasojevic-ne-ocekujem-uobicajeno-izborno-vece-nece-biti-spora-oko-rezultata-vec-oko-toga-koliko-su-izbori-bili-fer/",
        image: "/images/news/n1-3.webp",
      },
      {
        title: "Нови ДСС: Напредњаци шаљу своје људе да потпишу нашу листу",
        url: "https://n1info.rs/vesti/novi-dss-naprednjaci-salju-svoje-ljude-da-potpisu-nasu-listu/",
        image: "/images/news/n1-4.jpg",
      },
    ],
  },
  {
    source: "Nova.rs",
    items: [
      {
        title:
          "\"Како иде паковање и да ли ћете склонити 'ћацилend' и криминалце\": одговор Вучића новинарки Жаклини Таталовић",
        url: "https://nova.rs/vesti/politika/video-kako-ide-pakovanje-i-da-li-cete-skloniti-cacilend-i-kriminalce-pogledajte-kako-je-vucic-odgovorio-na-pitanje-novinarke-zakline-tatalovic/",
        image: "/images/news/nova-1.avif",
      },
      {
        title: "Вучић: Не треба еуфорија због пресуде Тачију и другима, суд хтео да сачува кредибилитет ОВК",
        url: "https://nova.rs/vesti/politika/vucic-ne-treba-euforija-zbog-presude-taciju-i-drugima-sud-hteo-da-sacuva-kredibilitet-ovk/",
        image: "/images/news/nova-2.avif",
      },
      {
        title:
          'ВИДЕО "Срце ми већ дуго слути, свему ће доћи крај": студенти објавили нови спот — најава новог скупа на Славији?',
        url: "https://nova.rs/vesti/politika/video-srce-mi-vec-dugo-sluti-svemu-ce-doci-kraj-studenti-objavili-novi-spot-da-li-je-ovo-najava-novog-skupa-na-slaviji/",
        image: "/images/news/nova-3.avif",
      },
      {
        title:
          '"Несторовићев покрет планирао коалицију са СНС после избора, како би Вулин добио министарство": реакције после снимка',
        url: "https://nova.rs/vesti/politika/nestorovicev-pokret-planirao-koaliciju-sa-sns-posle-izbora-kako-bi-vulin-dobio-ministarstvo-stizu-reakcije-posle-snimka-sa-sastanka-mi-snaga-naroda/",
        image: "/images/news/nova-4.avif",
      },
    ],
  },
  {
    source: "Blic",
    items: [
      {
        title: "Представници Странке правде и помирења предали РИК-у листу кандидата (ВИДЕО)",
        url: "https://www.blic.rs/vesti/politika/predstavnici-stranke-pravde-i-pomirenja-predali-rik-u-listu-kandidata/595x6st",
        image: "/images/news/blic-1.jpg",
      },
      {
        title: "РИК донео решења о образовању локалних изборних комисија за пет места у АП КиМ",
        url: "https://www.blic.rs/vesti/politika/rik-doneo-resenja-o-obrazovanju-lokalnih-izbornih-komisija-za-pet-mesta-u-ap-kim/r98pdt1",
        image: "/images/news/blic-2.jpg",
      },
      {
        title: "Вучић: Са места председника одлазим као частан човек који је верно служио свом народу (ВИДЕО)",
        url: "https://www.blic.rs/vesti/politika/vucic-sa-mesta-predsednika-odlazim-kao-castan-covek-koji-je-verno-sluzio-svom-narodu/fj5n71l",
        image: "/images/news/blic-3.jpg",
      },
      {
        title: "Исповест Драгице којој је ОВК убила мужа и сина (16), а њихове кости су нађене у масовној гробници",
        url: "https://www.blic.rs/vesti/politika/ispovest-dragice-kojoj-je-ovk-ubila-muza-i-sina-16-a-njihove-kosti-su-nadjene-u/wv7v4bl",
        image: "/images/news/blic-4.png",
      },
    ],
  },
  {
    source: "Informer",
    items: [
      {
        title: "Шок над шоковима! Шта све неће испливати о првом на блокадерској листи — Срдановић заговорник џендер идеологије",
        url: "https://informer.rs/politika/vesti/1151502/ilija-srdanovic-dzender-ideologija",
        image: "/images/news/informer-1.jpg",
      },
      {
        title: 'Прва вожња у озбиљној машини! Вучевић сео за волан, па поручио: "Ово није за игру" (ВИДЕО)',
        url: "https://informer.rs/politika/vesti/1151489/milos-vucevic-voznja-traktor",
        image: "/images/news/informer-2.png",
      },
      {
        title: "Ауууу... какав патос! Бранко Бабић објавио снимак, Срдановићу неће бити добро (ВИДЕО)",
        url: "https://informer.rs/politika/vesti/1151488/branko-babic-ilija-srdanovic",
        image: "/images/news/informer-3.jpg",
      },
      {
        title: "Сеча на Новој С! Сазнајемо: Јовићевић добио отказ, нека се спреми Слоба Георгиев",
        url: "https://informer.rs/politika/vesti/1151483/mihailo-jovicevic-slobodan-georgiev-otkazi",
        image: "/images/news/informer-4.jpg",
      },
    ],
  },
];
