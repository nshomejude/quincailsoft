import Link from 'next/link';
import {api,Me,permitted} from '@/lib/api';
import {Access} from '@/components/Access';
import {PageTitle,Records} from '@/components/Records';
import {Employee,Leave,period,reason,reviewStatus} from '@/lib/workforce';
import {WorkforceForm} from '../WorkforceForm';
export const dynamic='force-dynamic';
export default async function LeaveRegister({searchParams}:{searchParams:Promise<{from?:string;through?:string;offset?:string;status?:string;employeeId?:string}>}) {
 try {
  const query=await searchParams,{from,through,offset}=period(query),status=reviewStatus(query.status),employeeId=query.employeeId??'';
  const search=new URLSearchParams({from,through,offset:String(offset),limit:'25',status});if(employeeId)search.set('employeeId',employeeId);
  const [me,leaves]=await Promise.all([api<Me>('me'),api<Leave[]>('workforce/leave?'+search)]);
  const employee=employeeId&&permitted(me,'workforce:employees:read')?await api<Employee>('workforce/employees/'+encodeURIComponent(employeeId)):null;
  const page=(n:number)=>{const next=new URLSearchParams(search);next.set('offset',String(n));return '?'+next};
  return <><p className="breadcrumb"><Link href="/workforce">Workforce / Personnel</Link> / Leave / Congés</p><PageTitle title="Leave requests / Demandes de congé" description={employee?employee.name+' · '+employee.employeeNumber:'Requests and independent review for your active branch. / Demandes et vérification indépendante dans votre agence.'}/>
   <section className="panel"><form className="quick">{employeeId&&<input type="hidden" name="employeeId" value={employeeId}/>}<label>From / Du <input name="from" type="date" defaultValue={from} required/></label><label>Through / Au <input name="through" type="date" defaultValue={through} required/></label><label>Status / Statut <select name="status" defaultValue={status}><option value="">All / Tous</option>{['REQUESTED','APPROVED','REJECTED','CANCELLED'].map(s=><option key={s}>{s}</option>)}</select></label><button className="secondary">Filter / Filtrer</button>{employeeId&&<Link className="secondary" href="/workforce/leave">All employees / Tous les employés</Link>}</form>
    <Records rows={leaves} href="/workforce/leave" columns={[{key:'leaveType',label:'Leave type / Type de congé'},{key:'employeeId',label:'Employee / Employé'},{key:'from',label:'From / Du'},{key:'through',label:'Through / Au'},{key:'requestedMinutes',label:'Minutes requested / Minutes demandées'},{key:'status',label:'Status / Statut'}]}/>
    <div className="quick">{offset>0&&<Link className="secondary" href={page(Math.max(0,offset-25))}>Previous / Précédent</Link>}{leaves.length===25&&<Link className="secondary" href={page(offset+25)}>Next / Suivant</Link>}</div>
   </section>
   {permitted(me,'workforce:leave:write')&&<section className="panel"><h2>Request leave / Demander un congé</h2><p className="muted">No statutory entitlement or remaining balance is inferred. / Aucun droit légal ni solde disponible n’est calculé automatiquement.</p>{employee?<WorkforceForm path="leave" fixed={{employeeId:employee.id}} title="Submit request / Envoyer la demande" redirectBase="/workforce/leave" fields={[{name:'from',label:'From / Du',type:'date',value:from},{name:'through',label:'Through / Au',type:'date',value:through},{name:'leaveType',label:'Leave type / Type de congé',type:'select',options:[{value:'ANNUAL',label:'Annual / Annuel'},{value:'SICK',label:'Sick / Maladie'},{value:'UNPAID',label:'Unpaid / Sans solde'},{value:'OTHER',label:'Other / Autre'}]},{name:'requestedMinutes',label:'Requested minutes / Minutes demandées',type:'number',minimum:'1',maximum:'528480',hint:'Enter scheduled minutes for the inclusive date range. / Indiquez les minutes prévues sur la période, dates incluses.'},reason]}/>:<p><Link href="/workforce">Choose an employee / Sélectionner un employé</Link>, then open their leave requests.</p>}</section>}
  </>;
 }catch(error){return <Access error={error}/>}
}
