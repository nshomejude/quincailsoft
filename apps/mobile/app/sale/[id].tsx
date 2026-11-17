import {useEffect,useState} from 'react';
import {Text} from 'react-native';
import {useLocalSearchParams,router} from 'expo-router';
import {Page,Card,Action,styles} from '../../src/ui';
import {useSession,request} from '../../src/session';
type Sale={id:string;status:string;businessDate:string;totalXaf:string;version:number;warehouseId:string;customerId:string;lines:{id:string;productId:string;quantityMilli:string;unitPriceXaf:string;lineTotalXaf:string}[]};
export default function SaleDetail(){const {id}=useLocalSearchParams<{id:string}>();const {session}=useSession();const [sale,setSale]=useState<Sale|null>(null),[error,setError]=useState('');
async function refresh(){if(!session)return;try{setSale(await request<Sale>(session,'sales/'+encodeURIComponent(id)));setError('');}catch(e){setError(String(e));}}
useEffect(()=>{setSale(null);void refresh();},[session,id]);
return <Page title="Sale details"><Action title="Back to sales" onPress={()=>router.replace('/sales')}/>{error?<Text style={styles.error}>{error}</Text>:null}{!session?<Text>Sign in to view this sale.</Text>:<Action title="Refresh" onPress={()=>void refresh()}/>} {sale?<><Card><Text selectable>{sale.id}</Text><Text style={styles.text}>{sale.status} · {sale.totalXaf} FCFA</Text><Text>Date: {sale.businessDate}</Text><Text selectable>Customer: {sale.customerId}</Text><Text selectable>Warehouse: {sale.warehouseId}</Text><Text>Version: {sale.version}</Text></Card>{sale.lines.map(line=><Card key={line.id}><Text selectable>{line.productId}</Text><Text>{line.quantityMilli} milli units × {line.unitPriceXaf} FCFA / unit</Text><Text>Total: {line.lineTotalXaf} FCFA</Text><Action title="View product" onPress={()=>router.push({pathname:'/product/[id]',params:{id:line.productId}})}/></Card>)}</>:null}</Page>}
