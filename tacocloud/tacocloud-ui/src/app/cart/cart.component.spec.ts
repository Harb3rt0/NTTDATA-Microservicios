import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CartComponent } from './cart.component';
import { CartService } from './cart-service';

//TC-14 - Verifica el contrato de cantidad enviado por el carrito
describe('CartComponent TC-14', () => {
  let cart: CartService;
  let component: CartComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CartService]
    });

    cart = TestBed.get(CartService);
    http = TestBed.get(HttpTestingController);
    component = new CartComponent(cart, TestBed.get(HttpClient));
  });

  afterEach(() => http.verify());

  it('sends quantity two without client economic fields', () => {
    cart.addToCart({
      name: 'Lab taco',
      ingredients: [{id: 'FLTO', name: 'Flour Tortilla', unitPrice: 1.25}]
    });
    cart.getItemsInCart()[0].quantity = 2;
    component.payment = {cardNumber: '9999999999999999', expiration: '12/39', cvv: '999'};

    component.onSubmit();

    const tokenRequest = http.expectOne('http://localhost:8080/api/payment-methods/tokenize');
    tokenRequest.flush({id: 'PAYMENT1'});
    const orderRequest = http.expectOne('http://localhost:8080/api/orders');

    expect(orderRequest.request.body.items[0].quantity).toBe(2);
    expect(orderRequest.request.body.items[0].taco.ingredientIds).toEqual(['FLTO']);
    expect(orderRequest.request.body.total).toBeUndefined();
    expect(orderRequest.request.body.items[0].subtotal).toBeUndefined();
    expect(orderRequest.request.body.items[0].unitPriceAtPurchase).toBeUndefined();

    orderRequest.flush({id: 'ORDER1'});
    expect(cart.getItemsInCart().length).toBe(0);
  });
});
//Fin TC-14
