/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.Conecta4JFrame;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteTableroBarquitos extends Agent{
    
    Conecta4JFrame gui;
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
        
        //Regisro de la Ontología
        
        //Registro en Página Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("iowpengvoianpvganbanbribnoebir");
	sd.setName("aoengñvoianoerabvioearnbzspbn");
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se añaden las tareas principales.
        
    }
    
    /**
     * Finalzacion del Agente.
     */
    @Override
    protected void takeDown() {

        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se liberan los recuros y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    //Tareas del Agente Tablero de Conecta 4.
    
    /**
     * Tarea que solicita y procesa los mivimientos de los jugadores para jugar la partida (Contract Net).
     */
    public class TareaJugarPartidaTablero extends ContractNetInitiator{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param msg Mensaje que se enviara a los agentes receptores.
         */
        public TareaJugarPartidaTablero(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        /**
         * Funcion que procesa las respuestas de los agentes involucrados en el protocolo.
         * @param responses Vector con las respuestas recibidas del resto de agentes.
         * @param acceptances Vector que almacenara las respuestas que se enviaran a los agentes.
         */
        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            String movimiento = new String();
            //Se busca el mensaje del jugador que realizo el movimiento.
            for(int i=0; i<responses.size(); i++){
                ACLMessage mensaje = (ACLMessage) responses.get(i);
                if(mensaje.getPerformative() == ACLMessage.PROPOSE){
                    movimiento = mensaje.getContent();
                }
            }
            //Se responde a todos los jugadores con el ultimo movimiento realizado.
            for(int i=0; i<responses.size(); i++){
                ACLMessage mensaje = (ACLMessage) responses.get(i);
                ACLMessage respuesta = mensaje.createReply();
                respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                respuesta.setContent(movimiento);
                acceptances.add(respuesta);
            }
        }
        
    }
    
}
