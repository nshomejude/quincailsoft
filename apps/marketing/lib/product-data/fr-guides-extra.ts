import type { GuideSection } from "../product-types";

/** Sections supplémentaires insérées avant la section finale « Comment QuincaiSoft aide » de chaque guide. */
export const extras: Record<string, GuideSection[]> = {
  "track-cement-and-bulk-materials-fcfa": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["Quelques habitudes causent l'essentiel des ennuis avec les matériaux en vrac. Surveillez-les dans votre propre boutique."],
      list: [
        "Enregistrer une réception en tonnes et une vente en sacs sans conversion écrite.",
        "Arrondir une fraction de sac au lieu de chercher d'où elle vient.",
        "Corriger un comptage en modifiant discrètement une quantité, sans motif noté.",
        "Laisser plusieurs personnes utiliser des noms ou des unités différents pour le même produit.",
        "Remettre des sacs abîmés dans le stock vendable.",
      ],
    },
    {
      h2: "Une routine hebdomadaire simple",
      paras: [
        "Une fois par semaine, comptez vos cinq produits en vrac à rotation la plus rapide et comparez-les aux enregistrements. Notez les écarts et leurs causes probables. Une fois par mois, comptez tout et revoyez les produits qui montrent régulièrement des écarts. En quelques mois, vous verrez quels produits, quels moments de la journée et quelles personnes sont liés aux pertes, et vous pourrez agir sur des faits plutôt que sur des soupçons.",
      ],
    },
  ],
  "manage-customer-credit-hardware-shop": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["Les problèmes de crédit commencent petits et grossissent discrètement."],
      list: [
        "Accorder du crédit parce qu'un client insiste plutôt que parce qu'il est fiable.",
        "Garder les dettes dans un cahier que seule une personne comprend.",
        "Laisser le même client continuer d'acheter alors qu'une ancienne dette est impayée.",
        "Enregistrer un paiement sans dire quelle vente il règle.",
        "Traiter un acompte comme un revenu avant la livraison des marchandises.",
      ],
    },
    {
      h2: "Parler d'argent avec les clients",
      paras: [
        "Soyez clair et poli. Indiquez le montant dû, la vente concernée et la date d'échéance, et proposez d'envoyer un relevé écrit. Les clients qui se sentent respectés paient généralement plus vite. Si un client ne peut pas payer, convenez par écrit d'un plan réaliste et suspendez le crédit jusqu'à ce qu'il soit tenu. Une approche calme et documentée protège à la fois la relation et votre trésorerie.",
      ],
    },
  ],
  "buy-and-receive-stock-from-suppliers": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["La plupart des pertes d'achat sont simples et évitables."],
      list: [
        "Ajouter du stock à la passation de la commande au lieu de l'arrivée des marchandises.",
        "Signer un bon de livraison sans compter.",
        "Payer deux fois une facture parce que la référence n'a pas été enregistrée une seule fois.",
        "Laisser la même personne commander, réceptionner et approuver le paiement.",
        "Oublier d'enregistrer un retour fournisseur, si bien que le stock reste dans les livres.",
      ],
    },
    {
      h2: "Choisir et évaluer les fournisseurs",
      paras: [
        "Gardez une trace de la fiabilité de livraison, de l'exactitude et du prix de chaque fournisseur dans le temps. Comparez les offres pour vos produits principaux au moins de temps en temps, mais pesez le prix face à la fiabilité et aux conditions de paiement. Un prix un peu plus élevé auprès d'un fournisseur qui livre ce qui a été commandé, à temps, est souvent moins cher qu'un prix bas avec manques et retards. Revoyez vos principaux fournisseurs tous les quelques mois à partir des enregistrements que vous tenez déjà.",
      ],
    },
  ],
  "quotations-for-contractors": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["Les litiges sur les devis suivent souvent les mêmes schémas."],
      list: [
        "Omettre l'unité, si bien qu'un prix au sac est lu comme un prix à la tonne.",
        "Ne pas indiquer de durée de validité, puis devoir honorer un ancien prix.",
        "Modifier un devis après envoi au lieu d'émettre une révision.",
        "Oublier de mentionner que la livraison ou la découpe est en supplément.",
        "Ressaisir un devis accepté en vente et changer une quantité par inadvertance.",
      ],
    },
    {
      h2: "Construire une relation avec les entrepreneurs",
      paras: [
        "Les entrepreneurs reviennent chez les fournisseurs rapides, exacts et clairs. Répondez vite aux demandes de devis, expliquez les différences entre un devis et la facture finale, et gardez trace des projets passés de chaque entrepreneur pour préparer facilement les commandes répétées. Un processus de devis fiable est l'un des meilleurs moyens de gagner une clientèle professionnelle.",
      ],
    },
  ],
  "run-multiple-branches": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["La croissance révèle des habitudes qui étaient inoffensives dans une seule boutique."],
      list: [
        "Déplacer des marchandises entre succursales sur parole, sans rien enregistrer.",
        "Laisser un identifiant partagé servir à tout le monde.",
        "Utiliser des noms ou des unités de produits différents selon les succursales.",
        "Considérer comme normal l'écart de caisse d'une succursale.",
        "Ne regarder que le total et jamais les succursales une à une.",
      ],
    },
    {
      h2: "Avant d'ouvrir la succursale suivante",
      paras: [
        "Assurez-vous que la première succursale fonctionne avec des routines, pas avec la mémoire du propriétaire. Vérifiez que inventaires, clôtures de caisse et approbations y fonctionnent. Décidez qui dirige la nouvelle succursale, ce qu'il peut approuver et quels chiffres vous suivrez chaque semaine. Une succursale se supervise plus facilement quand les règles sont écrites avant son ouverture.",
      ],
    },
  ],
  "mobile-money-reconciliation-basics": [
    {
      h2: "Erreurs courantes à éviter",
      paras: ["Les erreurs de mobile money sont souvent petites, mais elles s'additionnent."],
      list: [
        "Accepter une capture d'écran comme preuve de paiement.",
        "Enregistrer un paiement comme encaissé avant la confirmation du fournisseur.",
        "Demander au client de payer à nouveau sans vérifier la première demande.",
        "Ignorer de petits écarts parce qu'ils semblent trop minimes pour compter.",
        "Mélanger le mobile money et les espèces dans un même total.",
      ],
    },
    {
      h2: "Que faire quand les chiffres ne concordent pas",
      paras: [
        "Ne modifiez pas vos totaux pour qu'ils concordent. Notez chaque écart avec sa référence, la date et ce que vous avez trouvé. S'il reste inexpliqué, interrogez le fournisseur avec la référence en main. Gardez la correction comme une écriture distincte et datée qui renvoie à l'originale, pour que quiconque relira vos livres plus tard puisse suivre ce qui s'est passé. Des contrôles réguliers et sereins rendent les grandes surprises rares.",
      ],
    },
  ],
};

/** Sections complémentaires, placées après les précédentes. */
export const more: Record<string, GuideSection[]> = {
  "run-multiple-branches": [
    {
      h2: "Une revue hebdomadaire que vous pouvez vraiment tenir",
      paras: [
        "Choisissez chaque semaine un moment fixe, même trente minutes, pour regarder la même page de chiffres pour chaque succursale. Posez chaque fois les trois mêmes questions : qu'est-ce qui a changé depuis la semaine dernière, qu'est-ce qui diffère des autres succursales, et qu'est-ce qui demande une décision. Notez les décisions et qui en est responsable. Une revue courte et régulière vaut mieux qu'une longue et occasionnelle, car les problèmes sont repérés quand les détails sont encore frais et que les personnes concernées peuvent encore les expliquer.",
      ],
    },
  ],
  "mobile-money-reconciliation-basics": [
    {
      h2: "Qui doit faire le rapprochement",
      paras: [
        "Idéalement, la personne qui rapproche le mobile money n'est pas celle qui encaisse les paiements au comptoir. Séparer les deux rend les erreurs plus faciles à repérer et supprime la tentation de cacher un écart. Dans une petite boutique où ce n'est pas possible, le propriétaire ou un responsable peut relire le rapprochement et le valider, même si quelqu'un d'autre l'a préparé. Quelle que soit la personne, notez la date, la période couverte et les écarts constatés.",
      ],
    },
  ],
  "buy-and-receive-stock-from-suppliers": [
    {
      h2: "Garder les documents ensemble",
      paras: [
        "Pour chaque achat, gardez liés la commande, le bon de livraison, l'enregistrement de réception et la facture fournisseur, pour que chacun puisse suivre une livraison de la demande au paiement. Quand un fournisseur conteste une quantité ou un prix, cette trace règle la question vite et calmement. Elle facilite aussi beaucoup votre revue annuelle des fournisseurs, car les faits sont déjà rassemblés.",
      ],
    },
  ],
  "quotations-for-contractors": [
    {
      h2: "Garder l'historique de vos devis",
      paras: [
        "Conservez chaque devis, y compris ceux qui n'ont pas été acceptés. L'historique de ce que vous avez proposé, quand et à quel prix, permet de répondre vite à un entrepreneur qui revient, de vérifier que les nouveaux prix restent cohérents avec les anciens, et de comprendre la fréquence à laquelle vos devis deviennent des ventes. Il vous protège aussi si un client prétend plus tard qu'un autre prix avait été promis.",
      ],
    },
  ],
};
