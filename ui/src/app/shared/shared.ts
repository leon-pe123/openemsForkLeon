import { routing, appRoutingProviders } from './../app.routing';
import { Dataset, EMPTY_DATASET } from './chart';
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';

export { Service, Utils, Websocket, Dataset, EMPTY_DATASET };

//TODO
export interface Log { }
export interface QueryReply { }
export interface ChannelAddresses { }
export class Data {
    summary: any
}
export interface Summary { }