import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { NgModule } from "@angular/core";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class Common_SavedEmissions { }
