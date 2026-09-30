import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

//TC-21 - Estado persistente de favoritos en la API
@Injectable()
export class FavoriteService {
  constructor(private http: HttpClient) { }

  list() {
    return this.http.get<any>('http://localhost:8080/api/users/me/favorites?page=0&size=50');
  }

  add(tacoId: string) {
    return this.http.put('http://localhost:8080/api/users/me/favorites/' + tacoId, {});
  }

  remove(tacoId: string) {
    return this.http.delete('http://localhost:8080/api/users/me/favorites/' + tacoId);
  }
}
//Fin TC-21
