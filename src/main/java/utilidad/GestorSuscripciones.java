/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilidad;

import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import java.util.HashSet;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class GestorSuscripciones extends HashSet<Subscription> implements SubscriptionManager {
  
    public GestorSuscripciones() {
       super();
    }

    @Override
    public boolean register(Subscription sub) throws RefuseException, NotUnderstoodException {
        //Se almacena la suscripcion recibida.
        this.add(sub);
        return true;
    }

    @Override
    public boolean deregister(Subscription sub) throws FailureException {
        //Eliminamos una subscipcion.
        this.remove(sub);
        return true;
    }
   
}
