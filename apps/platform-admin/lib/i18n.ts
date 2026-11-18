import fr from './fr.json' with {type:'json'};
export type Locale='en'|'fr';
export function translate(locale:Locale,value:string):string{
 if(locale==='en')return value;
 const key=value.trim(),translated=(fr as Record<string,string>)[key];
 if(translated)return value.replace(key,translated);
 const reference=key.indexOf(' Reference: ');if(reference>0)return translate(locale,key.slice(0,reference))+' Référence : '+key.slice(reference+12);
 const bounded=/^([A-Za-z]+): enter a value within (\d+) characters\.$/.exec(key);if(bounded)return `Champ ${bounded[1]} : saisissez une valeur de ${bounded[2]} caractères maximum.`;
 const invalid=/^([A-Za-z]+): invalid text\.$/.exec(key);if(invalid)return `Champ ${invalid[1]} : texte non valide.`;
 if(key.startsWith('Use an ISO timestamp with timezone for '))return 'Utilisez une date ISO avec fuseau horaire pour '+key.slice(37);
 if(key.startsWith('Reference: '))return value.replace('Reference: ','Référence : ');
 return value;
}
export function preferredLanguage(cookie:string|undefined,acceptLanguage:string|null):Locale{
 if(cookie==='en'||cookie==='fr')return cookie;
 const preferences=(acceptLanguage??'').split(',').map((entry,index)=>{const [tag,...params]=entry.trim().toLowerCase().split(';');const q=params.find(p=>p.trim().startsWith('q='));return {tag:tag.split('-')[0],quality:q?Number(q.trim().slice(2)):1,index};}).filter(p=>(p.tag==='en'||p.tag==='fr')&&Number.isFinite(p.quality)&&p.quality>0&&p.quality<=1).sort((a,b)=>b.quality-a.quality||a.index-b.index);
 return preferences[0]?.tag==='fr'?'fr':'en';
}
