export class CartItem {

  quantity = 1;

  taco: any;

  constructor(taco: any) {
    this.taco = taco;
  }

  //TC-14 - Estimacion visual basada en el catalogo recibido
  get unitPriceEstimate() {
    return this.taco.ingredients.reduce((total, ingredient) =>
      total + Number(ingredient.unitPrice || 0), 0);
  }

  get lineTotal() {
    return this.quantity * this.unitPriceEstimate;
  }
  //Fin TC-14

}
