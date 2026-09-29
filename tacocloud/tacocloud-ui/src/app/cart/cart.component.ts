import { Component, OnInit, Injectable } from '@angular/core';
import { CartService } from './cart-service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'taco-cart',
  templateUrl: 'cart.component.html',
  styleUrls: ['./cart.component.css']
})

@Injectable()
export class CartComponent implements OnInit {

  model = {
    deliveryName: '',
    deliveryStreet: '',
    deliveryState: '',
    deliveryZip: '',
    paymentMethodId: '', //modificacion para TC-12
    items: [] //modificacion para TC-14
  };

  //TC-12 - Datos efimeros enviados solo al endpoint de tokenizacion
  payment = {
    cardNumber: '',
    expiration: '',
    cvv: ''
  };
  //Fin TC-12

  constructor(private cart: CartService, private httpClient: HttpClient) {
    this.cart = cart;
  }

  ngOnInit() {}

  get cartItems() {
    return this.cart.getItemsInCart();
  }

  get cartTotal() {
    return this.cart.getCartTotal();
  }

  onSubmit() {
    //modificacion para TC-14
    this.model.items = this.cart.getItemsInCart().map(cartItem => ({
      taco: {
        name: cartItem.taco.name,
        ingredientIds: cartItem.taco.ingredients.map(ingredient => ingredient.id)
      },
      quantity: Number(cartItem.quantity)
    }));

    const headers = new HttpHeaders().set('Content-type', 'application/json')
        .set('Accept', 'application/json');
    this.httpClient.post<any>('http://localhost:8080/api/payment-methods/tokenize',
        this.payment, {headers: headers}).subscribe(paymentMethod => { //modificacion para TC-12
          this.model.paymentMethodId = paymentMethod.id;
          this.payment = {cardNumber: '', expiration: '', cvv: ''};
          this.httpClient.post('http://localhost:8080/api/orders', this.model,
              {headers: headers}).subscribe(r => this.cart.emptyCart());
        });

    // TODO: Do something after this...navigate to a thank you page or something
  }

}
