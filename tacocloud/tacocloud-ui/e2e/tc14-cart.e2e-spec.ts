import { browser, by, element } from 'protractor';

//TC-14 - La interfaz conserva una cantidad mayor a uno
describe('TC-14 cart quantity', () => {
  it('should retain quantity two in the cart', () => {
    browser.get('/cart');
    browser.executeScript(
      'var debug = ng.probe(document.querySelector("taco-cart"));' +
      'debug.componentInstance.cart.addToCart({' +
      'name:"Lab taco",ingredients:[{id:"FLTO",name:"Flour Tortilla",unitPrice:1.25}]});' +
      'debug.injector.get(ng.coreTokens.ApplicationRef).tick();');

    const quantity = element(by.css('taco-cart tbody select'));
    quantity.sendKeys('2');

    expect(quantity.getAttribute('value')).toContain('2');
    expect(browser.executeScript(
      'return ng.probe(document.querySelector("taco-cart"))' +
      '.componentInstance.cartItems[0].quantity;')).toEqual(2);
  });
});
//Fin TC-14
