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
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Motivo;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.aplicacion.conecta4.EstadoJuego;
import juegosTablero.aplicacion.conecta4.Ficha;
import juegosTablero.aplicacion.conecta4.JuegoConecta4;
import juegosTablero.aplicacion.conecta4.Movimiento;
import juegosTablero.aplicacion.conecta4.MovimientoEntregado;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteJugadorConecta4 extends Agent implements Vocabulario{
    
    public static final int NULL = -1;
    public static final int CASILLA_VACIA = 0;
    public static final int CASILLA_J1 = 2;
    public static final int CASILLA_J2 = 1;
    public static final int MAX_JUEGOS = 100;
    public static final int ALFA_INI = -99999999; //Valor inicial de alfa.
    public static final int BETA_INI = 99999999; //Valor inicial de beta.
    public static final int PROFUNDIDAD = 4; //Valor de la máxima profundidad que se va a alcanzar.
    
    // Para la generación y obtención del contenido de los mensages
    private final ContentManager manager = (ContentManager) getContentManager();
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologia;
    
    private Jugador jugador;
    private Juego juego;
    private Color color;
    private Random rand;
    private HashMap<String,int[][]> estadoTablero;
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
	jugador = new Jugador(this.getLocalName(), this.getAID());
        estadoTablero = new HashMap<>();
        rand = new Random();
        
        // Regisro de la Ontología
        try {
            ontologia = OntologiaJuegoConecta4.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
	manager.registerOntology(ontologia);
        
        //Registro en Paginas Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(TIPO_SERVICIO);
	sd.setName(NombreServicio.JUEGO_CONECTA_4.name());
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}catch(FIPAException fe){
            fe.printStackTrace();
	}
        
        //Template para la tarea de RecepcionProposicionJuego
        MessageTemplate temp = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        
        //Template para la tarea JugarPartida
        MessageTemplate temp2 = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),MessageTemplate.MatchPerformative(ACLMessage.CFP));
        
        //Se añaden las tareas principales.
        addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
        addBehaviour(new TareaJugarPartidaJugador(this, temp2));
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
    
    /**
     * Se registra un nuevo juego en la lista de juegos del jugador.
     * @param pj Elemento que contiene el juego a registrar.
     */
    private void registrarJuego(ProponerJuego pj){
        juego = pj.getJuego();
        JuegoConecta4 jc4 = (JuegoConecta4) pj.getTipoJuego();
        estadoTablero.put(juego.getIdJuego(), new int[jc4.getTablero().getDimX()][jc4.getTablero().getDimY()]);
        IniciarParametros(pj.getJuego());
    }
    
    /**
     * Se reinician los parametros al completar una ronda.
     * @param juego on el identificador del tablero a reiniciar.
     */
    private void IniciarParametros(Juego juego){
        color = null;
        ReiniciarTablero(juego);
    }

    /**
     * Se marcan todas las casillas del tablero como vacias.
     * @param juego on el identificador del tablero a reiniciar.
     */
    public void ReiniciarTablero(Juego juego){
        for(int i=0; i<estadoTablero.get(juego.getIdJuego()).length; i++){
            for(int j=0; j<estadoTablero.get(juego.getIdJuego())[i].length; j++){
                estadoTablero.get(juego.getIdJuego())[i][j] = CASILLA_VACIA;
            }
        }
    }

    /**
     * Se obtiene la primera fila libre de la columna que se pasa como parámetro.
     * @param columna Columna que se quiere comprobar.
     * @param juego Juego con el identificador del tablero a comprobar.
     * @return Coordenada de la primera fila libre en la columna.
     */
    public int getFila(int columna, Juego juego){
        for(int i=0; i<estadoTablero.get(juego.getIdJuego()).length; i++){
            if(estadoTablero.get(juego.getIdJuego())[estadoTablero.get(juego.getIdJuego()).length-i-1][columna] == CASILLA_VACIA){
                return estadoTablero.get(juego.getIdJuego()).length-i-1;
            }
        }
        return NULL;
    }
    
    // Se realiza un movimiento aleatorio.
//    private Posicion calcularJugada(Juego juego){
//        int columna = 0, fila = NULL;
//        while(fila == NULL){  //Modificar por el caclculo de la posicion.
//            columna = rand.nextInt(7);
//            fila = getFila(columna, juego);
//        }
//        Posicion pos = new Posicion(fila, columna);
//        return pos;
//    }
    
    /**
     * Se devuelve la posición en la que se realizará la jugada.
     * @param juego Juego con el identificador del tablero a comprobar.
     * @return Posicion dende realizar el movimiento.
     */
    private Posicion calcularJugada(Juego juego){
        int columna = 0;
        columna = alfaBeta(estadoTablero.get(juego.getIdJuego()));
        Posicion pos = new Posicion(getFila(columna, juego), columna);
        return pos;
    }
    
    /**
     * Función inicio del algoritmo AlfaBeta. Inicializa los valores de alfa y beta y obtiene las principales jugadas a efectuar.
     * @param tablero Representación del tablero de juego.
     * @param conecta Número de fichas consecutivas para ganar.
     * @return Columna en la que se realizará la jugada.
     */
    private int alfaBeta(int[][] tablero){
        //Obtiene una copia del tablero para trabajar facilmente con él.
        int[][] estado = tablero.clone();
        //Se inicializa una variable que almacenará la columna resultado.
        int columna = 0;
        //Se inicializa el alfa y alfa temporal (que almacenará el valor de comprobar las jugadas) para realizar las comprobaciones con la primera casilla de cada columna.
        int alfa = ALFA_INI, alfaTmp;
        //Se busca la posición más baja de cada columna (donde será colocada la ficha en caso de elegir dicha columna) y se inicia la poda alfa-beta con esos estados como origen.
        for(int i=0; i<estado.length; i++){
            int fila = getFilaEstado(i, estado);
            //Se altera la posición como si el rival hubiera colocado una ficha en dicha columna.
            estado[fila][i] = (color.ordinal()+1)%2;
            //Se obtiene el valor alfa de seleccionar esta columna. 
            alfaTmp = minValor(estado, i, fila, 0, ALFA_INI, BETA_INI);
            //Se restaura el valor de la posición alterada al original para evitar inconsistencias en el tablero.
            estado[fila][i] = CASILLA_VACIA;
            //Se comprueba si el valor alfa de la selección mejora el alfa global.
            //Si es así, se actualiza alfa y se toma dicha columna como solución temporal.
            if(alfaTmp > alfa){
                alfa = alfaTmp;
                columna = i;
            }
        }
        //Tras finalizar el bucle, se devuelve la columna más favorecedora.
        return columna;
    }
    
    /**
     * Función que evalúa la jugada desde el punto de vista de Min (Jugador). Busca la jugada que empeore el valor de beta y realiza la "poda" en caso necesario.
     * @param estado Matriz que representa el estado actual del tablero.
     * @param columna Columna en la que se realizó la última jugada.
     * @param fila Fila en la que se realizó la última jugada.
     * @param profundidad Profundidad de búsqueda alcanzada actualmente.
     * @param alfa Valor actual de alfa.
     * @param beta Valor actual de beta.
     * @return Valor de beta calculado en esta rama.
     */
    private int minValor(int[][] estado, int columna, int fila, int profundidad, int alfa, int beta){
        //Se comprueba si, en las condiciones actuales, algún jugador ha ganado la partida.
        if(ComprobarTablero(estado, fila, columna, color.ordinal()) == Estado.GANADOR){
            return Heuristica(estado, fila, columna, color.ordinal());
        }else{
            //Se comprueba si se ha alcanzado la profundidad límite para no continuar con el algortmo.
            if(profundidad > PROFUNDIDAD){
                return Heuristica(estado, fila, columna, color.ordinal());
            }else{
                //Se obtienen los valores de los próximos movimientos posibles y se analizan.
                for(int i=0; i<estado.length; i++){
                    int filaTmp = getFilaEstado(i, estado);
                    //Se altera la posición como si este jugador hubiera colocado una ficha en dicha columna.
                    estado[filaTmp][i] = color.ordinal();
                    //Se obtiene el valor beta de seleccionar esta columna.
                    int betaTmp = maxValor(estado, i, filaTmp, profundidad+1, alfa, beta);
                    //Se comprueba si el valor beta de la selección empeora el beta global.
                    //Si es así, se actualiza beta.
                    if(betaTmp < beta){
                        beta = betaTmp;
                    }
                    //Se restaura el valor de la posición alterada al original para evitar inconsistencias en el tablero.
                    estado[filaTmp][i] = CASILLA_VACIA;
                    //En el momento en que alfa sea mayor o igual a beta se corta la búsqueda por esta rama y se devuelve alfa para compararlo con el alfa del nivel superior.
                    if(alfa >= beta){
                        return alfa;
                    }
                }
                //Si se analiza toda la rama, se devuelve el valor actual de beta para compararlo con el alfa del nivel superior.
                return beta;
            }
        }
    }
    
    /**
     * Función que evalúa la jugada desde el punto de vista de Max (Rival). Busca la jugada que mejore el valor de alfa y realiza la "poda" en caso necesario.
     * @param estado Matriz que representa el estado actual del tablero.
     * @param columna Columna en la que se realizó la última jugada.
     * @param fila Fila en la que se realizó la última jugada.
     * @param profundidad Profundidad de búsqueda alcanzada actualmente.
     * @param alfa Valor actual de alfa.
     * @param beta Valor actual de beta.
     * @return Valor de alfa calculado en esta rama.
     */
    private int maxValor(int[][] estado, int columna, int fila, int profundidad, int alfa, int beta){
        //Se comprueba si, en las condiciones actuales, algún jugador ha ganado la partida.
        if(ComprobarTablero(estado, fila, columna, (color.ordinal()+1)%2) == Estado.GANADOR){
            return -Heuristica(estado, fila, columna, (color.ordinal()+1)%2);
        }else{
            //Se comprueba si se ha alcanzado la profundidad límite para no continuar con el algortmo.
            if(profundidad > PROFUNDIDAD){
                return -Heuristica(estado, fila, columna, (color.ordinal()+1)%2);
            }else{
                //Se obtienen los valores de los próximos movimientos posibles y se analizan.
                for(int i=0; i<estado.length; i++){
                    int filaTmp = getFilaEstado(i, estado);
                    //Se altera la posición como si el rival hubiera colocado una ficha en dicha columna.
                    estado[filaTmp][i] = (color.ordinal()+1)%2;
                    //Se obtiene el valor alfa de seleccionar esta columna.
                    int alfaTmp = minValor(estado, i, filaTmp, profundidad+1, alfa, beta);
                    //Se comprueba si el valor alfa de la selección mejora el alfa global.
                    //Si es así, se actualiza alfa.
                    if(alfaTmp > alfa){
                        alfa = alfaTmp;
                    }
                    //Se restaura el valor de la posición alterada al original para evitar inconsistencias en el tablero.
                    estado[filaTmp][i] = 0;
                    //En el momento en que alfa sea mayor o igual a beta se corta la búsqueda por esta rama y se devuelve beta para compararlo con el beta del nivel superior.
                    if(alfa >= beta){
                        return beta;
                    }
                }
                //Si se analiza toda la rama, se devuelve el valor actual de alfa para compararlo con el beta del nivel superior.
                return alfa;
            }
        }
    }
    
    /**
     * Se obtiene la primera fila libre del tablero de la columna que se pasa como parámetro.
     * @param columna Columna que se quiere comprobar.
     * @param estado Tablero que se quiere comprobar.
     * @return Primera fila libre.
     */
    public int getFilaEstado(int columna, int[][] estado){
        for(int i=0; i<estado.length; i++){
            if(estado[estado.length-i-1][columna] == CASILLA_VACIA){
                return estado.length-i-1;
            }
        }
        return NULL;
    }
    
    /**
     * Función que calcula la heurística de realizar un movimiento.
     * @param tablero Tablero que se quiere comprobar.
     * @param coordX Coordenada X del movimiento realizado.
     * @param coordY Coordenada Y del movimiento realizado.
     * @param jugador Jugador que realizó el movimiento.
     * @return Valor heurístico del movimiento.
     */
    public int Heuristica(int[][] tablero, int coordX, int coordY, int jugador){
        int coordXact, coordYact, cont, valor = 0;
        boolean fin;
        //Vertical.
        cont = 1;
        //Hacia arriba.
        coordXact = coordX;
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordYact--;
                cont++;
                valor+=cont;
            }else{
                fin = true;
            }
        }
        //Hacia abajo.
        coordXact = coordX;
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordYact++;
                cont++;
                valor+=cont;
            }else{
                fin = true;
            }
        }
        
        //Horizontal
        cont = 1;
        //Hacia la izquierda.
        coordXact = coordX-1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                cont++;  
                valor+=cont;
            }else{
                fin = true;
            }
        }
        //Hacia la derecha.
        coordXact = coordX+1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                cont++;
                valor+=cont;
            }else{
                fin = true;
            }
        }
        
        //Diagonal desde la izquierda-arriba
        cont = 1;
        //Hacia arriba.
        coordXact = coordX-1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact--;
                cont++; 
                valor+=cont;
            }else{
                fin = true;
            }
        }
        //Hacia la abajo.
        coordXact = coordX+1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact++;
                cont++;
                valor+=cont;
            }else{
                fin = true;
            }
        }
        
        //Diagonal desde la derecha-abajo
        cont = 1;
        //Hacia abajo.
        coordXact = coordX-1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact++;
                cont++;  
                valor+=cont;
            }else{
                fin = true;
            }
        }
        //Hacia arriba.
        coordXact = coordX+1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact--;
                cont++;
                valor+=cont;
            }else{
                fin = true;
            }
        }
        return valor;
    }

    /**
     * Función que comprueba si se ha ganado la partida.
     * @param tablero Tablero que se quiere comprobar.
     * @param coordX Coordenada X del movimiento realizado.
     * @param coordY Coordenada Y del movimiento realizado.
     * @param jugador Jugador que realizó el movimiento.
     * @return Estado actual de la partida.
     */
    public Estado ComprobarTablero(int[][] tablero, int coordX, int coordY, int jugador){
        int coordXact, coordYact, cont;
        boolean fin;
        //Vertical.
        cont = 1;
        //Hacia arriba.
        coordXact = coordX;
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia abajo.
        coordXact = coordX;
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Horizontal
        cont = 1;
        //Hacia la izquierda.
        coordXact = coordX-1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia la derecha.
        coordXact = coordX+1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Diagonal desde la izquierda-arriba
        cont = 1;
        //Hacia arriba.
        coordXact = coordX-1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia la abajo.
        coordXact = coordX+1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Diagonal desde la derecha-abajo
        cont = 1;
        //Hacia abajo.
        coordXact = coordX-1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia arriba.
        coordXact = coordX+1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < tablero.length && coordYact >= 0 && coordYact < tablero[0].length) && !fin){
            if((tablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        return Estado.SEGUIR_JUGANDO;
    }
    
    //Tareas del Agente Jugador de Conecta 4.
    
    /**
     * Tarea que recibe la propuesta de jugar de la Central de Juegos (Protocolo Request).
     */
    public class TareaRecepcionProposicionJuego extends ProposeResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea. 
         * @param mt Mensaje que se espera recibir.
         */
        public TareaRecepcionProposicionJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        /**
         * Función que recibe y procesa la petición de jugar una partida.
         * @param propose Mensaje con la partida propuesta.
         * @return Mensaje con la respuesta a la propuesta.
         * @throws NotUnderstoodException
         * @throws RefuseException
         */
        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
            try {
                Action ac = (Action) manager.extractContent(propose);
                ProponerJuego pj = (ProponerJuego) ac.getAction();
                if(estadoTablero.size() < MAX_JUEGOS){
                    registrarJuego(pj);
                    ACLMessage accept = propose.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    manager.fillContent(accept, new JuegoAceptado(pj.getJuego(), jugador));
                    return accept;
                }else{
                    ACLMessage reject = propose.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    manager.fillContent(reject, new Motivacion(pj.getJuego(),Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA));
                    return reject;
                }
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
              
            throw new NotUnderstoodException("Remitente desconocido.\n");
        }
        
    }
    
    /**
     * Tarea que indica las jugadas al tablero (Contract Net).
     */
    public class TareaJugarPartidaJugador extends ContractNetResponder{

        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param mt Mensaje que se espera recibir.
         */
        public TareaJugarPartidaJugador(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        /**
         * Funcion que procesa un mensaje CFP y acepta o rechaza la propuesta.
         * @param cfp Mensaje que se ha recibido.
         * @return Mensaje con el movimiento a realizar (o vacío sin no es su turno).
         * @throws RefuseException
         * @throws FailureException
         * @throws NotUnderstoodException
         */
        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            //Se obtiene el contenido del mensaje del tablero.
            try {
                Action ac = (Action) manager.extractContent(cfp);
                PedirMovimiento pm = (PedirMovimiento) ac.getAction();
                //si es el primer movimiento, se toma el color para el jugador.
                if(color == null){
                    if(pm.getJugadorActivo().getAgenteJugador().getName().equals(jugador.getAgenteJugador().getName())){
                        color = Color.ROJO;
                    }else{
                        color = Color.AZUL;
                    }
                }
                //Se construye la respuesta.
                ACLMessage accept = cfp.createReply();
                accept.setPerformative(ACLMessage.PROPOSE);
                //Si es su turno.
                if(pm.getJugadorActivo().getAgenteJugador().getName().equals(jugador.getAgenteJugador().getName())){
                    //Se realiza el movimiento.
                    Movimiento movimiento = new Movimiento(new Ficha(color), calcularJugada(pm.getJuego()));
                    manager.fillContent(accept, new MovimientoEntregado(pm.getJuego(), movimiento));
                }
                return accept;
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }
        
        /**
         * Procesa el resultado del movimiento recibido del Tablero.
         * @param cfp Mensaje de la Contract Net.
         * @param propose Mensaje de proposición del protocolo.
         * @param accept Mensaje con el movimiento anterior realizado.
         * @return Mensaje con el estado de la partida tras realizar el movimiento.
         * @throws FailureException
         */
        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            try {
                MovimientoEntregado me = (MovimientoEntregado) manager.extractContent(accept);
                Movimiento movComprobacion = new Movimiento(new Ficha(me.getMovimiento().getFicha().getColor()), new Posicion(getFila(me.getMovimiento().getPosicion().getCoorY(), me.getJuego()),me.getMovimiento().getPosicion().getCoorY()));
                estadoTablero.get(me.getJuego().getIdJuego())[movComprobacion.getPosicion().getCoorX()][movComprobacion.getPosicion().getCoorY()] = me.getMovimiento().getFicha().getColor().ordinal()+1;
                //Comprobar si se continua o se ha ganado la partida
                Estado estado = ComprobarTablero(estadoTablero.get(me.getJuego().getIdJuego()),movComprobacion.getPosicion().getCoorX(), movComprobacion.getPosicion().getCoorY(), movComprobacion.getFicha().getColor().ordinal()+1);
                if(estado == Estado.GANADOR){
                    if(me.getMovimiento().getFicha().getColor() != color){
                        estado = Estado.FIN_PARTIDA;
                    }
                    IniciarParametros(me.getJuego());
                }
                ACLMessage informDone = accept.createReply();
                informDone.setPerformative(ACLMessage.INFORM);
                manager.fillContent(informDone, new EstadoJuego(me.getJuego(), estado));
                return informDone;
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }    
        
    }
    
}
