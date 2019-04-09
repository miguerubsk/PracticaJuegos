/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tareas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.ArrayList;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class TareaBuscarAgentes extends TickerBehaviour{

    private ArrayList<AID> agentes;
    private String nombreServicio;
    
        public TareaBuscarAgentes(Agent a, long period, ArrayList<AID> _agentes, String _nombreServicio){
            super(a, period);
            agentes = _agentes;
            nombreServicio = _nombreServicio;
        }
        
        @Override
        protected void onTick(){
            
            //Buscar agentes
            DFAgentDescription aDescripcion = new DFAgentDescription();
            ServiceDescription sDescripcion = new ServiceDescription();
            sDescripcion.setName(nombreServicio);
            aDescripcion.addServices(sDescripcion);
            
            try{
                DFAgentDescription[] locAgentes = DFService.search(myAgent, aDescripcion);
                agentes.clear();
                //Si se han encontrado agentes
                if (locAgentes.length > 0) {
                    for(int i=0; i<locAgentes.length; i++) {
                        agentes.add(locAgentes[i].getName());
                    }
                }
            }catch(FIPAException e) {
		e.printStackTrace();
            }
        }
        
}
