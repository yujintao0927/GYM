import {reactive,markRaw} from 'vue';
import {SessionClient} from './auth';
import {Records} from './records';
export const client=reactive(new SessionClient(markRaw(fetch.bind(globalThis))));
export const records=reactive(new Records());
