import type { GuideData } from "../product-types";

export const guidesB: GuideData[] = [
  {
    slug: "quotations-for-contractors",
    icon: "file",
    keywords: ["devis", "entrepreneur", "acompte", "matériaux de construction", "FCFA", "du devis à la vente"],
    metaTitle: "Devis pour les entrepreneurs : guide pratique",
    description: "Comment établir des devis pour entrepreneurs et acheteurs de projets dans le commerce de matériaux : détailler clairement, fixer une validité, prendre un acompte et transformer le devis accepté en vente.",
    h1: "Devis pour les entrepreneurs",
    answer: "Un bon devis pour un entrepreneur liste chaque article avec son unité, sa quantité et son prix en FCFA, indique combien de temps les prix sont valables, précise ce qui est inclus ou non, et fixe les conditions de paiement, y compris l'éventuel acompte. À l'acceptation, convertissez ce même devis en vente pour que les prix et quantités convenus se reportent sans ressaisie.",
    sections: [
      {
        h2: "Pourquoi les entrepreneurs demandent des devis",
        paras: [
          "Les entrepreneurs achètent selon un budget et un calendrier. Ils doivent connaître le coût des matériaux avant de s'engager auprès d'un client, et ont souvent besoin d'un document à montrer au client ou à une banque. Un devis clair vous aide à gagner la commande et évite les discussions ultérieures sur ce qui avait été convenu.",
        ],
      },
      {
        h2: "Ce que doit contenir un devis",
        paras: [
          "Restez simple et complet. Quiconque le lit doit comprendre exactement ce qui est proposé, à quel prix et jusqu'à quand.",
        ],
        list: [
          "Le nom et les coordonnées de votre entreprise, le nom du client et le projet ou le chantier.",
          "Un numéro de devis unique et la date.",
          "Chaque article avec sa description, son unité, sa quantité, son prix unitaire et le total de la ligne, en FCFA.",
          "La durée de validité, après laquelle les prix peuvent changer.",
          "Ce qui est inclus et exclu, comme la livraison, la découpe ou le déchargement.",
          "Les conditions de paiement, y compris l'éventuel acompte et la date du solde.",
        ],
      },
      {
        h2: "Être exact sur les unités",
        paras: [
          "La plupart des litiges sur les devis portent sur les unités. Un prix au sac n'est pas un prix à la tonne. Écrivez l'unité à côté de chaque quantité et de chaque prix, et vérifiez que la quantité devisée est dans la même unité que le prix. Si l'entrepreneur pense en tonnes et que vous vendez en sacs, montrez les deux pour qu'il n'y ait aucun doute.",
        ],
      },
      {
        h2: "Fixer une durée de validité",
        paras: [
          "Les prix des matériaux changent. Indiquez la durée de validité du devis et respectez-la pendant cette période. Si un devis expire, émettez-en un nouveau aux prix actuels plutôt que de modifier l'ancien. Garder l'original inchangé vous protège tous les deux.",
        ],
      },
      {
        h2: "Demander un acompte sur les grosses commandes",
        paras: [
          "Un acompte montre l'engagement et vous aide à réserver ou commander le stock. Indiquez dans le devis le montant ou le pourcentage et son échéance. Enregistrez l'acompte comme de l'argent que vous détenez pour le client, puis affectez-le à la vente à la livraison. Dites clairement ce qui se passe si la commande est annulée.",
        ],
      },
      {
        h2: "Gérer commandes partielles et modifications",
        paras: [
          "Les entrepreneurs prennent souvent les matériaux par étapes selon l'avancement du chantier. Décidez si un devis peut être honoré en plusieurs livraisons et comment les quantités et prix restants sont protégés. Si l'entrepreneur veut changer des quantités, traitez-le comme une révision avec une nouvelle version, et gardez la précédente au dossier.",
        ],
      },
      {
        h2: "Transformer le devis accepté en vente",
        paras: [
          "Quand l'entrepreneur accepte, convertissez le même devis en vente au lieu de le ressaisir. Cela garde quantités et prix identiques et vous donne un lien clair du devis à la facture. Si les prix ont changé depuis le devis, ou s'il a expiré, arrêtez-vous et refaites un devis.",
        ],
      },
      {
        h2: "Relancer",
        paras: [
          "Tenez une liste des devis ouverts avec leur valeur et leur date d'expiration. Contactez le client avant l'expiration du devis, notez s'il a été accepté ou perdu, et pourquoi. Avec le temps, cela montre quels produits et quels prix font gagner des commandes.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft prépare les devis à partir des prix du catalogue avec une date d'expiration, les émet comme des instantanés numérotés non modifiables, et convertit un devis accepté en un brouillon de vente lié. La conversion est rejetée si les prix ont changé ou si le devis a expiré, et ne traite pas encore les devis comportant une taxe. L'acceptation partielle en plusieurs commandes et les plafonds de crédit par projet sont prévus.",
        ],
      },
    ],
    howTo: {
      name: "Comment établir un devis pour un entrepreneur et le transformer en vente",
      steps: [
        { name: "Lister chaque article exactement", text: "Donnez description, unité, quantité, prix unitaire et total en FCFA, avec l'unité écrite à côté de chaque chiffre." },
        { name: "Indiquer validité et conditions", text: "Donnez une date d'expiration, ce qui est inclus et exclu, et les conditions de paiement." },
        { name: "Demander un acompte sur les grosses commandes", text: "Enregistrez l'acompte comme de l'argent détenu pour le client et affectez-le à la livraison." },
        { name: "Convertir le devis accepté", text: "Créez la vente à partir du même devis pour que quantités et prix se reportent." },
        { name: "Relancer avant l'expiration", text: "Suivez les devis ouverts, contactez le client et notez pourquoi chacun a été gagné ou perdu." },
      ],
    },
    related: ["industries/construction-materials", "features", "resources/manage-customer-credit-hardware-shop"],
  },
  {
    slug: "run-multiple-branches",
    icon: "building",
    keywords: ["multi-succursales", "transfert de stock", "succursales", "entrepôts", "permissions", "gestion commerciale"],
    metaTitle: "Gérer plusieurs succursales : stock, caisse et contrôle",
    description: "Un guide pratique pour diriger plusieurs boutiques ou dépôts : séparer stock et caisse par succursale, transférer via le transit, définir les permissions et suivre les mêmes chiffres partout.",
    h1: "Gérer plusieurs succursales",
    answer: "Pour bien gérer plusieurs succursales, gardez stock, caisse et ventes séparés pour chacune, déplacez les marchandises entre succursales par un transfert enregistré qui passe par le transit, donnez à chaque personne l'accès aux seules succursales et actions dont elle a besoin, et suivez les mêmes quelques chiffres pour chaque succursale selon un rythme fixe. Sans ces habitudes, une deuxième boutique double souvent les problèmes plutôt que le bénéfice.",
    sections: [
      {
        h2: "Garder les enregistrements de chaque succursale séparés",
        paras: [
          "Chaque succursale a besoin de son propre stock, de ses propres caisses et de son propre registre de ventes. Les mélanger rend impossible de savoir laquelle performe ou où un manque a commencé. Vous pouvez toujours regarder l'ensemble de l'entreprise, mais les enregistrements sous-jacents doivent toujours dire à quelle succursale ils appartiennent.",
        ],
      },
      {
        h2: "Déplacer le stock par un transfert, pas par un appel téléphonique",
        paras: [
          "Quand des marchandises passent d'une succursale à une autre, enregistrez le transfert. Les marchandises parties d'une succursale et pas encore arrivées dans l'autre sont en transit. Elles ne doivent pas compter comme disponibles aux deux bouts. Cette seule règle évite de vendre deux fois le même article.",
        ],
        list: [
          "Rédigez le transfert avec produits et quantités.",
          "Faites-le approuver par une autre personne que le demandeur.",
          "Enregistrez l'expédition depuis la succursale source.",
          "Enregistrez la réception à destination, totale ou partielle, avec la quantité réellement reçue.",
        ],
      },
      {
        h2: "Rapprocher les écarts à la réception",
        paras: [
          "Quand la destination compte moins que ce qui a été expédié, l'écart doit être enregistré et examiné, pas absorbé. Décidez qui est responsable des marchandises en transit et dans quel délai les écarts doivent être signalés.",
        ],
      },
      {
        h2: "Donner aux personnes seulement l'accès dont elles ont besoin",
        paras: [
          "Un caissier d'une succursale n'a pas besoin de voir la caisse d'une autre. Un responsable peut voir sa succursale sans modifier les prix. Définissez quelques rôles, comme propriétaire, responsable de succursale, magasinier, achats et caissier, et donnez à chacun exactement les actions requises.",
          "Exigez une seconde personne pour approuver les actions sensibles : gros remboursements, grosses sorties de stock, changements de prix et réouverture d'une période clôturée. La personne qui lance l'action ne doit pas être celle qui l'approuve.",
        ],
      },
      {
        h2: "Clôturer la caisse de chaque succursale chaque jour",
        paras: [
          "Chaque caisse doit s'ouvrir avec un fonds connu et se fermer avec un montant compté, comparé à ce que les enregistrements attendent. Notez tout écart avec un motif. Le faire chaque jour, succursale par succursale, révèle les problèmes quand ils sont petits et que chacun se souvient de ce qui s'est passé.",
        ],
      },
      {
        h2: "Suivre les mêmes chiffres pour chaque succursale",
        paras: [
          "Choisissez une courte liste et regardez-la chaque semaine pour chaque succursale : ventes, marge, valeur du stock, écarts d'inventaire, sommes dues par les clients et écarts de caisse. Comparer les succursales entre elles est plus utile que d'en regarder une isolément.",
        ],
      },
      {
        h2: "Partager fournisseurs et prix, séparer les responsabilités",
        paras: [
          "Des achats centralisés peuvent obtenir de meilleures conditions, mais chaque succursale doit tout de même réceptionner et compter ce qui arrive. Gardez un seul catalogue et des unités cohérentes entre succursales pour que le même produit signifie la même chose partout. N'autorisez des exceptions par succursale que pour une raison précise.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft cadre les enregistrements par société et succursale, suit le stock par entrepôt, et prend en charge les transferts avec approbation, expédition en transit et réception partielle ou finale, ainsi que les inventaires approuvés, les sessions de caisse par caisse et les permissions par rôle avec approbations indépendantes. La création en libre-service de sociétés et succursales et les vues consolidées multi-sociétés sont prévues.",
        ],
      },
    ],
    howTo: {
      name: "Comment gérer plusieurs succursales",
      steps: [
        { name: "Garder les enregistrements de chaque succursale séparés", text: "Donnez à chaque succursale son propre stock, ses caisses et son registre de ventes." },
        { name: "Déplacer le stock par un transfert enregistré", text: "Rédigez, approuvez, expédiez et réceptionnez, pour que les marchandises en transit ne soient pas disponibles aux deux bouts." },
        { name: "Donner aux personnes seulement l'accès dont elles ont besoin", text: "Définissez quelques rôles et exigez un second approbateur pour les actions sensibles." },
        { name: "Clôturer la caisse de chaque succursale chaque jour", text: "Comparez espèces comptées et attendues et notez chaque écart avec un motif." },
        { name: "Suivre les mêmes chiffres pour chaque succursale", text: "Regardez chaque semaine ventes, marge, écarts de stock, créances et écarts de caisse." },
      ],
    },
    related: ["industries/wholesale-distribution", "features", "security", "resources/buy-and-receive-stock-from-suppliers"],
  },
  {
    slug: "mobile-money-reconciliation-basics",
    icon: "phone",
    keywords: ["mobile money", "rapprochement", "MTN Mobile Money", "Orange Money", "paiements", "FCFA"],
    metaTitle: "Bases du rapprochement mobile money pour les commerces",
    description: "Les bases du rapprochement des paiements mobile money dans un commerce : noter les références, distinguer saisi et confirmé, comparer trois sources et enquêter sur les écarts.",
    h1: "Bases du rapprochement mobile money",
    answer: "Pour rapprocher le mobile money, enregistrez chaque paiement avec sa référence du fournisseur, ne considérez un paiement comme encaissé que lorsque le fournisseur le confirme, et comparez régulièrement trois sources : vos propres enregistrements de ventes, la liste des transactions du fournisseur et votre relevé bancaire ou de portefeuille. Enquêtez sur chaque écart, paiement manquant, en double, tardif ou partiel, au lieu d'ajuster vos totaux pour qu'ils concordent.",
    sections: [
      {
        h2: "Pourquoi le rapprochement compte",
        paras: [
          "Le mobile money est pratique, mais il ajoute un second jeu d'enregistrements hors de votre boutique : celui du fournisseur. Un client peut vous montrer un message qui ressemble à un paiement. Un retard réseau peut faire arriver un paiement plus tard. Sans contrôles réguliers, vous pouvez vous retrouver avec des ventes jamais encaissées, ou de l'argent que vous ne savez pas expliquer.",
        ],
      },
      {
        h2: "Comprendre ce que l'on compare",
        paras: [
          "Le rapprochement compare ce que vous croyez avoir encaissé avec ce qui est réellement arrivé. Il y a trois vues, et chacune peut diverger des autres pour une bonne raison.",
        ],
        list: [
          "Vos enregistrements de ventes : ce que vous avez noté comme payé par mobile money.",
          "Les enregistrements du fournisseur : les transactions et les éventuels lots de règlement qu'il communique.",
          "Votre relevé bancaire ou de portefeuille : ce qui est arrivé sur votre compte, après frais.",
        ],
      },
      {
        h2: "Étape 1 : noter une référence pour chaque paiement",
        paras: [
          "Pour chaque vente par mobile money, enregistrez le montant, la date, le client ou la vente concernés et la référence de transaction du fournisseur. Un paiement sans référence ne peut pas être vérifié plus tard.",
        ],
      },
      {
        h2: "Étape 2 : distinguer saisi et confirmé",
        paras: [
          "Une référence saisie à la main n'a pas été vérifiée, même si elle paraît correcte. Marquez-la comme non confirmée jusqu'à ce que le fournisseur confirme le paiement, et ne remettez pas de marchandises de grande valeur contre un paiement non confirmé. Lorsque votre système peut interroger le fournisseur sur le statut, attendez une confirmation correspondant au montant et à la devise avant de considérer le paiement comme encaissé.",
          "Méfiez-vous des captures d'écran et des messages transférés. Ils sont faciles à falsifier et ne valent pas confirmation.",
        ],
      },
      {
        h2: "Étape 3 : gérer les résultats incertains",
        paras: [
          "Il arrive qu'une demande soit envoyée sans réponse claire. Ne débitez pas le client une seconde fois. Vérifiez d'abord le statut de la demande d'origine, à l'aide de sa référence. Ce n'est que si le fournisseur indique un échec que vous pouvez redemander le paiement.",
        ],
      },
      {
        h2: "Étape 4 : comparer régulièrement",
        paras: [
          "À la fin de chaque jour ou de chaque semaine, listez les ventes mobile money que vous avez enregistrées et rapprochez chacune de la liste du fournisseur par référence et montant. Puis comparez les totaux du fournisseur à ce qui est arrivé sur votre compte.",
        ],
      },
      {
        h2: "Étape 5 : enquêter sur les écarts",
        paras: [
          "Chaque type d'écart a une cause probable. Traitez-les un par un et notez le résultat.",
        ],
        list: [
          "Enregistré mais non confirmé : le paiement a peut-être échoué ou été retardé.",
          "Confirmé mais non enregistré : une vente a peut-être été oubliée.",
          "Doublon : le même paiement a peut-être été enregistré deux fois.",
          "Montant différent : paiement partiel, trop-perçu ou frais.",
          "Annulé : le fournisseur l'a peut-être annulé ou contesté.",
        ],
      },
      {
        h2: "Garder les remboursements honnêtes",
        paras: [
          "Ne marquez pas un remboursement mobile money comme effectué avant que le fournisseur ne le confirme. Si une demande de remboursement échoue ou n'est pas claire, gardez-la ouverte et vérifiez son statut avant de réessayer.",
        ],
      },
      {
        h2: "Comment QuincaiSoft aide",
        paras: [
          "QuincaiSoft est conçu pour qu'un paiement mobile money ne soit affiché comme vérifié que lorsque le fournisseur le confirme, et qu'une demande peu claire soit réinterrogée par la même référence au lieu d'être débitée deux fois. Un adaptateur MTN Mobile Money existe mais exige des identifiants réels et l'acceptation du fournisseur. Orange Money n'est pas intégré. L'import des règlements du fournisseur pour un rapprochement complet à trois vues est prévu.",
        ],
      },
    ],
    howTo: {
      name: "Comment rapprocher les paiements mobile money",
      steps: [
        { name: "Noter une référence pour chaque paiement", text: "Notez montant, date, vente et référence de transaction du fournisseur." },
        { name: "Distinguer saisi et confirmé", text: "Marquez les références saisies à la main comme non confirmées jusqu'à confirmation du fournisseur." },
        { name: "Gérer les résultats incertains", text: "Vérifiez le statut de la demande d'origine avant de redemander un paiement au client." },
        { name: "Comparer régulièrement", text: "Rapprochez vos paiements enregistrés de la liste du fournisseur, puis comparez les totaux du fournisseur à votre compte." },
        { name: "Enquêter sur les écarts", text: "Traitez paiements manquants, en double, tardifs, partiels et annulés, et notez chaque résultat." },
      ],
    },
    faq: [
      { q: "La capture d'écran d'un message de paiement est-elle une preuve de paiement ?", a: "Non. Les captures d'écran et messages transférés sont faciles à falsifier. Ne considérez un paiement comme encaissé que lorsque le fournisseur le confirme." },
    ],
    related: ["payments", "features", "resources/manage-customer-credit-hardware-shop"],
  },
];
