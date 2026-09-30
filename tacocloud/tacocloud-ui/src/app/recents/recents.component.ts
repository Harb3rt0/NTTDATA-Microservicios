import { Component, OnInit, Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { HttpClient } from '@angular/common/http';
import { FavoriteService } from './FavoriteService';

@Component({
  selector: 'recent-tacos',
  templateUrl: 'recents.component.html',
  styleUrls: ['./recents.component.css']
})

@Injectable()
export class RecentTacosComponent implements OnInit {
  recentTacos: any;
  favoriteIds: any = {};

  constructor(private httpClient: HttpClient, private favoriteService: FavoriteService) { }

  ngOnInit() {
    this.httpClient.get<any>('http://localhost:8080/api/tacos?page=0&size=12&sort=createdAt&direction=desc') //modificacion para TC-19
        .subscribe(data => this.recentTacos = data.items);
    this.favoriteService.list().subscribe(data => data.items.forEach(favorite =>
      this.favoriteIds[favorite.tacoId] = true), () => {}); //modificacion para TC-21
  }

  //TC-21 - Alterna el favorito persistido
  toggleFavorite(taco: any) {
    const action = this.favoriteIds[taco.id]
      ? this.favoriteService.remove(taco.id) : this.favoriteService.add(taco.id);
    action.subscribe(() => this.favoriteIds[taco.id] = !this.favoriteIds[taco.id]);
  }
  //Fin TC-21
}
