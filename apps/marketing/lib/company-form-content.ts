import type { Lang } from "./site";

export type TopicKey = "sales" | "demo" | "support" | "partnership" | "press" | "other";

export const TOPICS: Record<Lang, Record<TopicKey, string>> = {
  en: { sales: "Pricing or pilot", demo: "Demo request", support: "Support", partnership: "Partnership", press: "Press or media", other: "Something else" },
  fr: { sales: "Tarifs ou pilote", demo: "Demande de démo", support: "Assistance", partnership: "Partenariat", press: "Presse ou médias", other: "Autre sujet" },
};

export type FormText = {
  name: string; email: string; business: string; role: string; country: string; branches: string; topic: string; message: string;
  optional: string; choose: string; branchOptions: string[]; consent: string; consentShort: string; privacy: string; honeypot: string;
  submit: string; submitDemo: string; sending: string; note: string; noscript: string; sent: string; failed: string; fallbackLead: string; rate: string; origin: string; hintContact: string; hintDemo: string;
  errRequired: string; errEmail: string; errConsent: string; errSummary: string;
};

export const FORM: Record<Lang, FormText> = {
  en: {
    name: "Your name", email: "Email", business: "Business name", role: "Your role", country: "Country", branches: "Number of branches",
    topic: "Topic", message: "Message", optional: "optional", choose: "Choose", branchOptions: ["1", "2 to 5", "6 to 20", "More than 20"],
    consent: "I agree that Opesware Technologies may use these details to reply to my request. See the", consentShort: "Consent", privacy: "privacy notice",
    honeypot: "Leave this field empty",
    submit: "Send message", submitDemo: "Send demo request",
    sending: "Sending...",
    note: "Your message is sent by email to the Opesware Technologies team. This website does not keep a copy of it.",
    noscript: "This form needs JavaScript. Without it, write to us directly by email at",
    sent: "Thank you. Your message was sent and we will reply to",
    failed: "We could not send your message from this page right now. Please send it by email instead:",
    fallbackLead: "Open an email with your details filled in",
    rate: "Too many messages were sent from your connection. Please wait a few minutes and try again, or write by email:",
    origin: "Your message could not be verified. Please write to us by email:",
    hintContact: "Tell us your type of business, what you sell and what you want to improve.",
    hintDemo: "Tell us your type of business, the number of branches, how you track stock and sales today, and what you most want to see.",
    errRequired: "This field is required.", errEmail: "Enter an email address such as name@example.com.", errConsent: "Please tick the box to continue.",
    errSummary: "Please correct the following:",
  },
  fr: {
    name: "Votre nom", email: "E-mail", business: "Nom de l'entreprise", role: "Votre fonction", country: "Pays", branches: "Nombre de succursales",
    topic: "Sujet", message: "Message", optional: "facultatif", choose: "Choisir", branchOptions: ["1", "2 à 5", "6 à 20", "Plus de 20"],
    consent: "J'accepte qu'Opesware Technologies utilise ces informations pour répondre à ma demande. Voir la", consentShort: "Consentement", privacy: "notice de confidentialité",
    honeypot: "Laissez ce champ vide",
    submit: "Envoyer le message", submitDemo: "Envoyer la demande de démo",
    sending: "Envoi en cours...",
    note: "Votre message est envoyé par e-mail à l'équipe d'Opesware Technologies. Ce site n'en conserve pas de copie.",
    noscript: "Ce formulaire nécessite JavaScript. Sans lui, écrivez-nous directement par e-mail à",
    sent: "Merci. Votre message a été envoyé et nous répondrons à",
    failed: "Nous n'avons pas pu envoyer votre message depuis cette page pour le moment. Envoyez-le plutôt par e-mail :",
    fallbackLead: "Ouvrir un e-mail avec vos informations pré-remplies",
    rate: "Trop de messages ont été envoyés depuis votre connexion. Patientez quelques minutes puis réessayez, ou écrivez par e-mail :",
    origin: "Votre message n'a pas pu être vérifié. Écrivez-nous par e-mail :",
    hintContact: "Indiquez votre type d'activité, ce que vous vendez et ce que vous voulez améliorer.",
    hintDemo: "Indiquez votre type d'activité, le nombre de succursales, comment vous suivez aujourd'hui le stock et les ventes, et ce que vous voulez voir en priorité.",
    errRequired: "Ce champ est obligatoire.", errEmail: "Saisissez une adresse e-mail telle que nom@exemple.com.", errConsent: "Veuillez cocher la case pour continuer.",
    errSummary: "Veuillez corriger les points suivants :",
  },
};
