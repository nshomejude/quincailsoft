import type { GuideData } from "../product-types";

export const guidesA: GuideData[] = [
  {
    slug: "track-cement-and-bulk-materials-fcfa",
    icon: "ruler",
    keywords: ["stock de ciment", "matériaux en vrac", "unités de mesure", "FCFA", "quincaillerie", "inventaire"],
    metaTitle: "Suivre le ciment et les matériaux en vrac en FCFA",
    description: "Un guide pratique pour suivre ciment, sable, fer à béton et autres matériaux en vrac : choisir une unité de base, convertir avec exactitude, inventorier régulièrement et valoriser le stock en FCFA.",
    h1: "Suivre le ciment et les matériaux en vrac en FCFA",
    answer: "Pour suivre fiablement le ciment et les autres matériaux en vrac, choisissez une unité de base par produit, comme le sac pour le ciment, enregistrez chaque réception et chaque vente dans cette unité, ne convertissez entre unités qu'avec une arithmétique exacte, inventoriez à intervalles réguliers et valorisez le stock en FCFA entiers à votre coût moyen. La plupart des pertes viennent du mélange des unités, pas de la matière elle-même.",
    sections: [
      {
        h2: "Pourquoi les matériaux en vrac posent problème",
        paras: [
          "Le ciment arrive en tonnes sur le camion, se stocke en sacs et se vend en sacs, en demi-sacs ou au poids. Le fer à béton arrive en bottes et se vend à la barre ou coupé sur mesure. À chaque changement d'unité, une quantité peut être mal lue, arrondie ou enregistrée deux fois.",
          "L'objectif du suivi est simple : à tout moment, le chiffre de vos enregistrements doit correspondre à ce qui se trouve physiquement dans la cour, et vous devez savoir pourquoi quand ce n'est pas le cas.",
        ],
      },
      {
        h2: "Étape 1 : choisir une unité de base pour chaque produit",
        paras: [
          "L'unité de base est la plus petite unité que vous voulez compter exactement. Pour le ciment, c'est généralement le sac. Pour les clous, ce peut être le kilogramme ou la pièce. Pour les tôles, c'est la tôle.",
          "Choisissez-la une fois et gardez-la. Changer l'unité d'un produit qui a déjà un historique de stock est ce qui rend les enregistrements peu fiables : décidez donc soigneusement avant la première réception.",
        ],
      },
      {
        h2: "Étape 2 : écrire les conversions",
        paras: [
          "Listez toutes les autres unités dans lesquelles vous achetez ou vendez, et leur rapport avec l'unité de base. Par exemple, si un sac de ciment pèse 50 kg, une tonne vaut 20 sacs. Une botte de barres peut être un nombre fixe de pièces. Écrivez la règle et mettez-vous d'accord avec votre fournisseur et votre personnel.",
          "Une bonne règle est exacte : la conversion doit donner un nombre entier d'unités de base. Si une livraison de 1,5 tonne donne 30 sacs, c'est parfait. Si une conversion donne une fraction d'unité de base, arrêtez-vous et cherchez pourquoi au lieu d'arrondir.",
        ],
      },
      {
        h2: "Étape 3 : enregistrer les réceptions contre le bon de livraison",
        paras: [
          "Comptez ce qui est réellement livré, dans l'unité de base, et comparez avec le bon de livraison et la commande. Enregistrez la quantité que vous avez comptée, pas celle écrite sur le papier. Notez séparément les sacs abîmés pour qu'ils n'entrent pas dans le stock vendable.",
        ],
        list: [
          "Comptez à l'arrivée, si possible en présence du chauffeur.",
          "Notez le fournisseur, la date et le numéro du bon de livraison.",
          "Gardez les articles abîmés ou manquants hors du stock vendable tant que le fournisseur n'a pas donné son accord.",
        ],
      },
      {
        h2: "Étape 4 : gérer la matière libre et reconditionnée",
        paras: [
          "Il arrive que le vrac soit vendu en libre ou reconditionné en colis plus petits. Traitez la matière libre et chaque taille de colis comme des produits distincts, et déplacez le stock de l'un à l'autre par une étape de reconditionnement enregistrée. La quantité qui sort du produit en vrac doit être égale à celle qui entre dans les colis, plus la perte que vous mesurez et notez.",
          "N'ajustez jamais la quantité d'un produit parce que vous avez reconditionné vers un autre sans enregistrement. C'est ainsi que le stock disparaît sur le papier.",
        ],
      },
      {
        h2: "Étape 5 : inventorier selon un calendrier",
        paras: [
          "Comptez souvent les produits à rotation rapide et de forte valeur, et moins souvent les autres. Un inventaire doit comparer ce que vous trouvez avec ce que disent les enregistrements, et noter l'écart avec un motif. Enquêtez sur les grands écarts avant de les corriger.",
          "Faites l'inventaire à un moment calme, ou notez les ventes réalisées pendant le comptage, pour que les mouvements ne faussent pas le résultat.",
        ],
      },
      {
        h2: "Étape 6 : valoriser le stock en FCFA",
        paras: [
          "Valorisez le stock à ce qu'il vous a coûté, avec une méthode constante comme le coût moyen pondéré. Gardez les montants en FCFA entiers, et figez le coût d'une vente au moment de la vente pour que des changements de prix ultérieurs ne réécrivent pas les marges passées.",
          "Quand les frais de transport ou de manutention sont importants, décidez comment les inclure et appliquez la même règle à chaque fois.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft garde une unité de base par produit avec des quantités exactes, des inventaires approuvés qui comptabilisent gains et pertes, et des tâches approuvées de reconditionnement et de découpe. La conversion automatique entre unités comme sacs et tonnes est prévue et n'est pas disponible aujourd'hui. Voir la page des fonctionnalités pour l'état de chaque module.",
        ],
      },
    ],
    howTo: {
      name: "Comment suivre le ciment et les matériaux en vrac en FCFA",
      steps: [
        { name: "Choisir une unité de base par produit", text: "Prenez la plus petite unité que vous voulez compter exactement, comme le sac pour le ciment, et gardez-la." },
        { name: "Écrire les conversions", text: "Listez les autres unités et leur rapport exact avec l'unité de base, par exemple 20 sacs par tonne pour des sacs de 50 kg." },
        { name: "Enregistrer les réceptions contre le bon de livraison", text: "Comptez ce qui est livré dans l'unité de base et gardez les articles abîmés hors du stock vendable." },
        { name: "Gérer la matière libre et reconditionnée", text: "Traitez la matière libre et chaque taille de colis comme des produits distincts et déplacez le stock par une étape enregistrée." },
        { name: "Inventorier selon un calendrier", text: "Comparez le stock compté aux enregistrements et notez chaque écart avec un motif." },
        { name: "Valoriser le stock en FCFA", text: "Utilisez une méthode de coût constante comme la moyenne pondérée et figez le coût de chaque vente." },
      ],
    },
    related: ["industries/construction-materials", "features", "resources/buy-and-receive-stock-from-suppliers"],
  },
  {
    slug: "manage-customer-credit-hardware-shop",
    icon: "wallet",
    keywords: ["crédit client", "créances", "acomptes", "quincaillerie", "FCFA", "recouvrement"],
    metaTitle: "Gérer le crédit client dans une quincaillerie",
    description: "Un guide pratique pour vendre à crédit dans une quincaillerie : à qui accorder du crédit, comment l'enregistrer, comment prendre des acomptes et encaisser sans litiges.",
    h1: "Gérer le crédit client dans une quincaillerie",
    answer: "Pour gérer le crédit client en sécurité, n'en accordez qu'à des clients que vous connaissez, fixez un plafond pour chacun, enregistrez chaque vente à crédit comme une créance liée à la vente, prenez des acomptes pour les grosses commandes, appliquez chaque paiement à des dettes précises et passez en revue chaque semaine qui doit quoi. Le crédit est un prêt sur votre propre trésorerie : il demande le même soin que l'argent liquide.",
    sections: [
      {
        h2: "Décider qui obtient du crédit",
        paras: [
          "Le crédit fonctionne mieux pour des clients réguliers dont vous connaissez l'activité : entrepreneurs avec des chantiers en cours, institutions, voisins de longue date. Soyez plus prudent avec les acheteurs occasionnels de grosses commandes.",
          "Écrivez une règle simple que le personnel peut appliquer sans vous appeler à chaque fois : qui peut se voir proposer du crédit, jusqu'à quel montant, et qui peut approuver une exception.",
        ],
      },
      {
        h2: "Fixer un plafond et un délai",
        paras: [
          "Donnez à chaque client à crédit un plafond qui reflète ce que vous pourriez vous permettre de perdre, pas ce qu'il aimerait acheter. Convenez de la date de paiement attendue. Un plafond sans délai n'est qu'une demi-politique.",
          "Pour contrôler le plafond, comptez tout ce que le client vous doit sur toutes les succursales et tous les projets, pas seulement la dernière vente.",
        ],
      },
      {
        h2: "Enregistrer chaque vente à crédit comme une créance",
        paras: [
          "Une vente à crédit doit réduire votre stock et créer un montant dû par le client, lié à cette vente et à sa facture numérotée. Évitez un cahier de montants épars : quand un paiement arrive, vous devez savoir quelle vente il règle.",
          "Le crédit n'étant pas de l'argent liquide, il ne doit pas apparaître dans la caisse. Votre comptage de caisse et vos créances sont deux choses différentes, et les mélanger cache des problèmes dans les deux.",
        ],
      },
      {
        h2: "Utiliser les acomptes pour les grosses commandes",
        paras: [
          "Pour une grosse commande, demandez un acompte avant de commander ou de réserver la marchandise. Un acompte n'est pas encore une vente. C'est de l'argent que vous détenez pour le client et que vous devez rendre en marchandises ou en espèces. Enregistrez-le comme tel et affectez-le à la vente à la remise des marchandises, pour ne pas compter deux fois le même argent comme un revenu.",
          "Si le client annule, décidez à l'avance de ce qu'il advient de l'acompte, et gardez tout remboursement approuvé et enregistré, avec le solde restant vérifié.",
        ],
      },
      {
        h2: "Appliquer chaque paiement à des dettes précises",
        paras: [
          "Quand un client paie, enregistrez le montant, la date, le moyen de paiement et la ou les ventes réglées. Un paiement supérieur à la dette est un signal d'alerte : ce peut être une erreur ou une avance, et il faut le traiter explicitement plutôt que le cacher.",
        ],
        list: [
          "Remettez un reçu pour chaque paiement.",
          "N'enregistrez jamais deux fois le même paiement.",
          "Pour le mobile money, vérifiez la confirmation du fournisseur avant de considérer le paiement comme encaissé.",
        ],
      },
      {
        h2: "Passer en revue les dettes chaque semaine",
        paras: [
          "Une fois par semaine, listez les clients qui vous doivent, combien, et depuis quand. Relancez d'abord les dettes les plus anciennes, et suspendez le crédit à un client en retard jusqu'à ce qu'il paie. Faites-le calmement et par écrit si possible : un relevé de ce qui est dû évite la plupart des litiges.",
        ],
      },
      {
        h2: "Gérer proprement retours et litiges",
        paras: [
          "Si des marchandises reviennent, liez le retour à la vente d'origine et émettez un avoir, pour que le solde du client baisse du bon montant et que le stock ne revienne que si les articles sont revendables. N'effacez pas la vente d'origine. Les corrections doivent être de nouvelles écritures qui renvoient à l'ancienne.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft enregistre les ventes à crédit comme des créances, applique les encaissements en espèces à la dette reconnue et rejette les trop-perçus, conserve les acomptes comme dettes envers les clients et les affecte aux ventes, et prend en charge les remboursements d'acomptes approuvés. Plafonds de crédit, balance âgée et relevés clients sont prévus et ne sont pas encore disponibles.",
        ],
      },
    ],
    howTo: {
      name: "Comment gérer le crédit client dans une quincaillerie",
      steps: [
        { name: "Décider qui obtient du crédit", text: "Écrivez une règle simple : qui peut se voir proposer du crédit, jusqu'à quel montant, et qui approuve les exceptions." },
        { name: "Fixer un plafond et un délai", text: "Donnez à chaque client à crédit un plafond et une date de paiement convenue, en comptant tout ce qu'il doit déjà." },
        { name: "Enregistrer chaque vente à crédit comme une créance", text: "Liez le montant dû à la vente et à sa facture numérotée, et gardez-le hors de la caisse." },
        { name: "Prendre des acomptes pour les grosses commandes", text: "Conservez l'acompte comme dette envers le client et affectez-le à la vente à la remise." },
        { name: "Appliquer chaque paiement à des dettes précises", text: "Notez montant, date, moyen et vente réglée, et remettez un reçu." },
        { name: "Passer en revue les dettes chaque semaine", text: "Listez qui doit quoi et depuis quand, et suspendez le crédit aux clients en retard." },
      ],
    },
    related: ["industries/hardware-shops", "features", "payments", "resources/mobile-money-reconciliation-basics"],
  },
  {
    slug: "buy-and-receive-stock-from-suppliers",
    icon: "truck",
    keywords: ["achats", "réception de marchandises", "facture fournisseur", "bon de commande", "stock", "gros"],
    metaTitle: "Acheter et réceptionner le stock des fournisseurs",
    description: "Un guide pratique des achats pour un commerce ou un grossiste : commander avec approbation, réceptionner contre la livraison, rapprocher la facture fournisseur et gérer les retours.",
    h1: "Acheter et réceptionner le stock des fournisseurs",
    answer: "Pour acheter et réceptionner du stock sans pertes, émettez une commande avec un fournisseur nommé, des quantités et des coûts convenus, faites-la approuver par une autre personne que l'acheteur, comptez chaque livraison par rapport à la commande à son arrivée, n'acceptez en stock que ce qui a réellement été reçu, rapprochez la facture du fournisseur de la réception, et payez à l'échéance ce qui est dû. Commander n'ajoute pas de stock. Réceptionner, si.",
    sections: [
      {
        h2: "Séparer commande, réception et facturation",
        paras: [
          "Trois événements différents se produisent quand vous achetez : vous commandez, les marchandises arrivent et le fournisseur facture. Traitez-les comme des enregistrements distincts. Le stock doit augmenter à la réception, pas à la commande, et une dette envers le fournisseur doit apparaître quand vous acceptez sa facture.",
          "Garder ces étapes séparées permet de repérer les problèmes courants : marchandises commandées mais jamais livrées, livrées mais non facturées, et factures pour des marchandises jamais reçues.",
        ],
      },
      {
        h2: "Étape 1 : émettre une commande claire",
        paras: [
          "Une commande doit nommer le fournisseur, chaque produit et quantité, le coût unitaire convenu et la date de livraison attendue. Gardez les quantités dans les mêmes unités de base que votre stock, pour qu'aucune conversion ne soit nécessaire à l'arrivée des marchandises.",
          "Faites approuver la commande par une autre personne que celle qui l'a passée, au moins au-delà d'un montant que vous fixez. Cette seule habitude évite beaucoup d'erreurs et la plupart des fraudes internes.",
        ],
      },
      {
        h2: "Étape 2 : réceptionner contre la commande",
        paras: [
          "À l'arrivée de la livraison, comptez-la et comparez-la au bon de livraison et à la commande. Enregistrez la quantité réellement reçue. Les livraisons sont souvent partielles : autorisez plusieurs réceptions pour une commande et tenez un solde courant de ce qui reste attendu.",
        ],
        list: [
          "Comptez dans l'unité de base, pas dans celle du fournisseur, si elles diffèrent.",
          "Vérifiez l'état et mettez de côté les marchandises abîmées.",
          "N'acceptez pas plus que commandé sans modification approuvée.",
        ],
      },
      {
        h2: "Étape 3 : rapprocher la facture fournisseur",
        paras: [
          "Quand la facture arrive, rapprochez-la d'une réception précise. Vérifiez quantités et coûts unitaires par rapport à ce que vous avez commandé et reçu. Les écarts se règlent avec le fournisseur avant d'enregistrer la dette.",
          "Enregistrez une seule fois le numéro de facture du fournisseur. Une facture en double est un moyen courant d'être payé deux fois : chaque référence fournisseur doit donc être unique.",
        ],
      },
      {
        h2: "Étape 4 : payer et suivre ce que vous devez",
        paras: [
          "Tenez une liste de ce que vous devez à chaque fournisseur et de l'échéance. Payez des factures précises pour toujours savoir lesquelles sont réglées. Les retards de paiement peuvent vous coûter des conditions ou une priorité, et connaître tôt vos dettes aide à planifier la trésorerie.",
        ],
      },
      {
        h2: "Étape 5 : retourner ce qui ne va pas",
        paras: [
          "Si des marchandises sont défectueuses ou erronées, retournez-les avec un enregistrement lié à la réception d'origine. Vous ne pouvez pas retourner plus que ce que vous avez reçu. Diminuez votre stock de la quantité retournée, et convenez avec le fournisseur d'un avoir ou d'un remplacement.",
        ],
      },
      {
        h2: "Connaître votre coût",
        paras: [
          "Chaque réception vous dit ce que les marchandises ont coûté. Utilisez une méthode constante pour valoriser le stock, comme le coût moyen pondéré, afin que le coût d'une vente ultérieure reflète ce que vous avez vraiment payé. Si le transport ou la douane s'ajoutent au coût, décidez comment les inclure et appliquez la règle à chaque fois.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft prend en charge les fiches fournisseurs, brouillons d'achat, approbation indépendante, réceptions partielles qui ne peuvent dépasser la commande, factures fournisseurs rapprochées des réceptions avec références uniques, et retours de marchandises non facturées. L'affectation des paiements fournisseurs, la balance âgée, les frais d'approche et les demandes d'achat sont prévus et ne sont pas encore disponibles.",
        ],
      },
    ],
    howTo: {
      name: "Comment acheter et réceptionner le stock des fournisseurs",
      steps: [
        { name: "Émettre une commande claire", text: "Nommez le fournisseur, les produits, quantités, coûts convenus et la date attendue, et faites-la approuver par une autre personne." },
        { name: "Réceptionner contre la commande", text: "Comptez chaque livraison par rapport au bon de livraison et à la commande, enregistrez la quantité réelle et autorisez les réceptions partielles." },
        { name: "Rapprocher la facture fournisseur", text: "Rapprochez la facture d'une réception précise et vérifiez quantités et coûts avant d'enregistrer la dette." },
        { name: "Payer et suivre ce que vous devez", text: "Payez des factures précises et tenez une liste de ce qui est dû." },
        { name: "Retourner ce qui ne va pas", text: "Liez les retours à la réception d'origine et ne retournez jamais plus que ce qui a été reçu." },
      ],
    },
    related: ["industries/wholesale-distribution", "features", "resources/run-multiple-branches"],
  },
];
