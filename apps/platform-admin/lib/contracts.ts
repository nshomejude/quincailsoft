export type RequestItem={id:string;displayName:string;requestedBy:string;approvedBy:string|null;status:string;reason:string;version:string;createdAt:string};
export type Profile={legalName:string;registrationNumber:string;address:string;ownerSubject:string;ownerEmail:string;region:string;timezone:string;locale:string;documentPrefix:string;openingMode:string;privacyVersion:string;termsVersion:string;planCode:string};
export type Configuration={requestId:string;profile:Profile;configuredBy:string;approvedBy:string|null;status:string;version:string;identityVerifiedAt:string|null;identityIssuer:string|null;createdAt:string};
export type Provisioned={requestId:string;tenantId:string;companyId:string;branchId:string;warehouseId:string;ownerSubject:string;status:string;version:string;trialEndsAt:string;completedSteps:string[];authorizationEpoch:string};
export type Invoice={id:string;tenantId:string;kind:string;originalId:string|null;number:string;issuerName:string;issuerTaxReference:string;customerName:string;customerTaxReference:string;description:string;jurisdiction:string;periodStart:string;periodEnd:string;dueDate:string;subtotalXaf:string;discountXaf:string;taxXaf:string;totalXaf:string;createdAt:string};
export type FormState={ok:boolean;message:string};
export const initialFormState:FormState={ok:false,message:''};
