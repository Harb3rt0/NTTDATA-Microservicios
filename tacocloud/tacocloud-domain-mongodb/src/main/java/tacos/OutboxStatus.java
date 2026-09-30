package tacos;

//TC-29 - Estados recuperables del outbox
public enum OutboxStatus {
  NEW,
  PUBLISHING,
  PUBLISHED,
  FAILED
}
//Fin TC-29
