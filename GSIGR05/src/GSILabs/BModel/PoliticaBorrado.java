package GSILabs.BModel;

/**
 * Política configurable para mantener la coherencia del modelo cuando se
 * elimina una entidad que tiene dependientes.
 *
 * <p>El propio modelo aplica esta política a través del método público
 * {@code eliminar(PoliticaBorrado)}, presente en
 * {@link Local#eliminar(PoliticaBorrado)},
 * {@link Cliente#eliminar(PoliticaBorrado)},
 * {@link Dueño#eliminar(PoliticaBorrado)},
 * {@link Review#eliminar(PoliticaBorrado)},
 * {@link Reserva#eliminar(PoliticaBorrado)} y
 * {@link Contestación#eliminar(PoliticaBorrado)}. Cada uno de esos
 * métodos considera dependientes distintos: un {@link Local} depende de
 * sus {@link Local#getReviews() reviews} y {@link Local#getReservas()
 * reservas} vinculadas; un {@link Cliente} depende de sus
 * {@link Cliente#getReviews() reviews} y {@link Cliente#getReservas()
 * reservas}; un {@link Dueño} depende de sus
 * {@link Dueño#getContestaciones() contestaciones} y de los locales
 * vinculados de los que es único propietario (no de los que comparte con
 * otros dueños); una {@link Review} depende de su
 * {@link Review#getContestacion() contestación}, si la tiene; y una
 * {@link Reserva} y una {@link Contestación} no tienen dependientes
 * propios, por lo que la política no afecta a su resultado.</p>
 *
 * <p>Con {@link #BLOQUEAR}, si la entidad tiene algún dependiente,
 * {@code eliminar(PoliticaBorrado)} lanza una
 * {@link DominioException}, cuyo mensaje explica qué lo impide, sin
 * modificar nada. Con {@link #CASCADA}, elimina también, recursivamente,
 * a los dependientes
 * de la entidad, y en ambos casos devuelve un conjunto de solo lectura,
 * en orden de eliminación, con todas las entidades eliminadas (incluida
 * la propia), para que {@code GSILabs.BSystem.BusinessSystem}, que es
 * quien gestiona el almacenamiento y conoce todas las entidades del
 * sistema, actualice sus colecciones en consecuencia. En el caso de
 * {@link Dueño}, tanto en {@link #BLOQUEAR} como en {@link #CASCADA}, si
 * no se lanza excepción el dueño se retira de los locales que comparte
 * con otros dueños, de forma que nunca queda un local sin ningún dueño
 * (C06).</p>
 */
public enum PoliticaBorrado {

    /**
     * Rechaza el borrado con una excepción documentada
     * ({@link DominioException}) si la entidad tiene algún
     * dependiente, sin modificar nada.
     */
    BLOQUEAR,

    /**
     * Permite el borrado eliminando también, en cascada, a los
     * dependientes de la entidad borrada.
     */
    CASCADA
}
