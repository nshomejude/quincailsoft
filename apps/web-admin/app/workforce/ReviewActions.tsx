import {WorkforceForm} from './WorkforceForm';
import {reason} from '@/lib/workforce';

/** Java revalidates authority, version, maker/checker and affected payroll on every command. */
export function ReviewActions({path,status,version,independentReviewer,canApprove,canCancel=false}:{path:string;status:string;version:string;independentReviewer:boolean;canApprove:boolean;canCancel?:boolean}) {
 return <div>
  {status==='REQUESTED'&&canApprove&&(independentReviewer?<>
   <details><summary>Approve / Approuver</summary><WorkforceForm key={path+version+'approve'} path={path+'/approve'} fixed={{version}} fields={[reason]} title="Approve / Approuver"/></details>
   <details><summary>Reject / Rejeter</summary><WorkforceForm key={path+version+'reject'} path={path+'/reject'} fixed={{version}} fields={[reason]} title="Reject / Rejeter"/></details>
  </>:<p className="notice">An independent reviewer must decide this request. / Une autre personne habilitée doit examiner cette demande.</p>)}
  {canCancel&&['REQUESTED','APPROVED'].includes(status)&&<details><summary>Cancel / Annuler</summary><p className="muted">Recorded payroll may prevent cancellation. / Une paie déjà enregistrée peut empêcher l’annulation.</p><WorkforceForm key={path+version+'cancel'} path={path+'/cancel'} fixed={{version}} fields={[reason]} title="Cancel / Annuler"/></details>}
 </div>;
}
