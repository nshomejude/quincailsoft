'use client';
import {useActionState} from 'react';
import {supportAction} from './actions';
import {supportText} from './copy';
export default function SupportForm({children,label,confirmation,locale}:{children:React.ReactNode;label:string;confirmation:string;locale:'en'|'fr'}){
 const [state,submit,pending]=useActionState(supportAction,{ok:false,message:''});const tr=(value:string)=>supportText(locale,value);
 return <form action={submit}><fieldset disabled={pending||state.ok} style={{border:0,padding:0}}>{children}<label style={{display:'flex',alignItems:'start',gap:12}}><input type="checkbox" required style={{width:20,minHeight:20,marginTop:3}}/>{tr(confirmation)}</label><button>{tr(pending?'Submitting…':label)}</button></fieldset>{state.message&&<div className={state.ok?'notice':'error'} role={state.ok?'status':'alert'}>{tr(state.message)}{state.ok&&<p><a href="">{tr('Reload latest state')}</a></p>}</div>}</form>;
}
