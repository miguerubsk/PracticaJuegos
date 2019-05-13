/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import java.util.ArrayList;
import java.util.HashMap;
import jade.util.leap.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.Vocabulario.TipoJuego;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.ColocarBarcos;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.barcos.Localizacion;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author pedroj
 */
public class AgenteJugadorBarquitos extends Agent implements Vocabulario{
    private Ontology ontologiaBarcos;
    private Jugador jugador;
    private int juegosActivos;
    private Random rand = new Random(System.currentTimeMillis());
    private final Codec codec = new SLCodec();
    private final ContentManager managerBarcos = (ContentManager) getContentManager();
    private HashMap<String, int[][][]> Tableros;
    
    
    

    @Override
    protected void setup() {
        System.out.println("Inicia la ejecución de " + this.getName());
        jugador = new Jugador(this.getLocalName(), this.getAID());
        Tableros = new HashMap<String, int[][][]>();
            
        
        try {
            ontologiaBarcos = OntologiaJuegoBarcos.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        managerBarcos.registerLanguage(codec);
	managerBarcos.registerOntology(ontologiaBarcos);
        
        
        // Registro en las páginas Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(TIPO_SERVICIO);
	sd.setName(NombreServicio.JUEGO_BARCOS.name());
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        
        juegosActivos = 0;
        JuegoBarcos juegoBarcos = new JuegoBarcos();
        
        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
	}
        
        
   

    @Override
    protected void takeDown() {
        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    public class TareaRecepcionProposicionJuego extends ProposeResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param mt Mensaje que se espera recibir.
         */
        public TareaRecepcionProposicionJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {	
            Boolean temp = false;        
            
            
            try {
                        Action ac = (Action) managerBarcos.extractContent(propose);
                        ProponerJuego pj = (ProponerJuego) ac.getAction();
//                        if(pj.getJuego().getTipoJuego()!= Vocabulario.TipoJuego.BARCOS){
//                            ACLMessage refuse = propose.createReply();
//                            refuse.setPerformative(ACLMessage.REJECT_PROPOSAL);
//                            managerBarcos.fillContent(refuse, new Motivacion(pj.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO));
//                            return refuse;
//                        }else if (juegosActivos<=2){
//                            ACLMessage refuse = propose.createReply();
//                            refuse.setPerformative(ACLMessage.REJECT_PROPOSAL);
//                            managerBarcos.fillContent(refuse, new Motivacion(pj.getJuego(), Motivo.JUEGOS_ACTIVOS_SUPERADOS));
//                            return refuse;
//                        }else{
//                            ACLMessage accept = propose.createReply();
//                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
//                            managerBarcos.fillContent(accept, new JuegoAceptado(pj.getJuego(), jugador));
//                            return accept;
//                        }
                        if(temp){
                            ACLMessage accept = propose.createReply();
                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            managerBarcos.fillContent(accept, new JuegoAceptado(pj.getJuego(), jugador));
                            return accept;
                        }else{
                            ACLMessage refuse = propose.createReply();
                            refuse.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            managerBarcos.fillContent(refuse, new Motivacion(pj.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO));
                            return refuse;
                        }
                    } catch (Codec.CodecException | OntologyException e) {
                            e.printStackTrace();
                    }
            return null;
        }
    }
    
    public class TareaJugarPartida extends ContractNetResponder{
        
        public TareaJugarPartida(Agent a, MessageTemplate mt) {
            super(a, mt);
        }
        
        
        
    }
    
    public PosicionBarcos colocarBarcos(ColocarBarcos juego){
        PosicionBarcos posiciones = new PosicionBarcos();
        posiciones.setJuego(juego.getJuego());
        Localizacion locations = null;
        Posicion coord = null;
        ArrayList localizacionBarcos[] = new ArrayList[10];
        for(int i=0; i<10; i++){
            localizacionBarcos[i]=new ArrayList();
        }
        
        coord.setCoorX(1);
        coord.setCoorY(1);
        locations.setBarco(TipoBarco.FRAGATA);
        locations.setOrientacion(Orientacion.HORIZONTAL);
        locations.setPosicion(coord);
        localizacionBarcos[0].add(locations);
        
        rand = new Random(System.currentTimeMillis());
        
        
        List listaPosiciones = new jade.util.leap.ArrayList(localizacionBarcos[rand.nextInt(10)]);
        posiciones.setLocalizacionBarcos(listaPosiciones);
        
        return posiciones;
    }
}
