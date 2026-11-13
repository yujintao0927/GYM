export type Kind='weight'|'food'|'training';
export interface Draft {kind:Kind;occurredAt:string;value:string;name:string;portion:string}
export interface RecordItem extends Draft {id:string}
export function errors(d:Draft){const e:Partial<Record<keyof Draft,string>>={};if(!d.occurredAt||!Number.isFinite(Date.parse(d.occurredAt)))e.occurredAt='请选择有效的记录时间';if(d.kind!=='food'&&(!d.value.trim()||!Number.isFinite(Number(d.value))||Number(d.value)<=0))e.value='请输入大于零的有限数值';if(d.kind!=='weight'&&(!d.name.trim()||d.name.trim().length>80))e.name='请输入1至80字的名称';if(d.kind==='food'&&(!d.portion.trim()||d.portion.trim().length>120))e.portion='请输入1至120字的分量';return e}
export class Records {items:RecordItem[]=[];clear(){this.items=[]}add(d:Draft){if(Object.keys(errors(d)).length)throw new Error('请检查记录字段');const item={...d,value:d.kind==='food'?'':d.value,name:d.kind==='weight'?'':d.name.trim(),portion:d.kind==='food'?d.portion.trim():'',id:crypto.randomUUID()};this.items.unshift(item);return item}}

