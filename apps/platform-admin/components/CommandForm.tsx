'use client';
import {translate as t} from '@/lib/i18n';
import {useLanguage} from '@/components/Language';

import {useActionState} from 'react';
import {initialFormState,type FormState} from '@/lib/contracts';
export default function CommandForm({action,children,label,confirm}:{action:(state:FormState,data:FormData)=>Promise<FormState>;children:React.ReactNode;label:string;confirm?:string}){const locale=useLanguage();
 const [state,submit,pending]=useActionState(action,initialFormState);
 return <form action={submit} className="command-form"><fieldset disabled={pending||state.ok}>{children}{confirm&&<label className="check"><input type="checkbox" required/>{confirm}</label>}<button type="submit">{pending?t(locale,'Submitting…'):label}</button></fieldset>{state.message&&<div className={state.ok?'notice success':'notice error'} role={state.ok?'status':'alert'}>{t(locale,state.message)}{state.ok&&<p><a href="">{t(locale,"Reload latest state")}</a></p>}</div>}{pending&&<p className="muted" role="status">{t(locale,"Awaiting the service result. Do not submit another operation.")}</p>}</form>;
}
