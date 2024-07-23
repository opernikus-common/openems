// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RefresherCustomEvent } from '@ionic/angular';
import { Subject } from 'rxjs';
import { DataService } from 'src/app/shared/genericComponents/shared/dataservice';
import { Edge, EdgeConfig, Service, Utils, Websocket, Widgets } from 'src/app/shared/shared';

@Component({
  selector: 'live',
  templateUrl: './live.component.html',
})
export class LiveComponent implements OnInit, OnDestroy {

  public edge: Edge = null;
  public config: EdgeConfig = null;
  public widgets: Widgets = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();
  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => this.dataService.refresh(ev);

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
    protected websocket: Websocket,
    private dataService: DataService,
  ) { }

  public ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.currentEdge.subscribe((edge) => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
    });
  }

  public ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
