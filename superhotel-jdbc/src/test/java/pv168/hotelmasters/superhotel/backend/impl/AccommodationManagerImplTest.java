package pv168.hotelmasters.superhotel.backend.impl;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import javax.sql.DataSource;

import pv168.hotelmasters.superhotel.backend.db.Utilities;
import pv168.hotelmasters.superhotel.backend.entities.Accommodation;
import pv168.hotelmasters.superhotel.backend.entities.Guest;
import pv168.hotelmasters.superhotel.backend.entities.Room;
import pv168.hotelmasters.superhotel.backend.exceptions.InvalidEntityException;
import pv168.hotelmasters.superhotel.backend.exceptions.ValidationError;
import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static java.time.Month.*;

/**
 * @author Gabriela Godiskova
 */
public class AccommodationManagerImplTest {

    private AccommodationManagerImpl manager;
    private GuestManagerImpl guestManager;
    private RoomManagerImpl roomManager;
    private DataSource dataSource;

    private final static LocalDate NOW = LocalDate.of(2016,FEBRUARY,29);
    private final static Clock clock = Clock.fixed(
            NOW.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static DataSource prepareDataSrc() throws SQLException {
        EmbeddedDataSource dataSrc = new EmbeddedDataSource();
        dataSrc.setDatabaseName("memory:superHotel-test");
        dataSrc.setCreateDatabase("create");
        return dataSrc;
    }

    @Before
    public void setUp() throws SQLException{
        dataSource = prepareDataSrc();
        Utilities.executeSql(getClass().getResource("createTables.sql"),dataSource);
        manager = new AccommodationManagerImpl(clock);
        manager.setDataSource(dataSource);
        guestManager = new GuestManagerImpl(clock);
        guestManager.setDataSource(dataSource);
        roomManager = new RoomManagerImpl();
        roomManager.setDataSource(dataSource);
        prepareTestData();
    }

    @After
    public void tearDown() throws SQLException {
        Utilities.executeSql(getClass().getResource("dropTables.sql"),dataSource);
    }

    private Guest john,jane,jack,phoebe,jefrey,guestNotInDB;
    private Room economy,luxury,penthouse;

    private void prepareTestData() throws SQLException {
        john = new GuestFactory()
                .name("john")
                .address("Manesova 120, Brno")
                .birthday(LocalDate.of(1996,NOVEMBER,23))
                .crCardNm(1234L).build();
        jane = new GuestFactory()
                .name("jane")
                .address("Filkukova 42, Brno")
                .birthday(LocalDate.of(2008,FEBRUARY,29))
                .crCardNm(12345L)
                .build();
        jack = new GuestFactory().name("jack")
                .address("Hrncirska 23, Brno")
                .birthday(LocalDate.of(1997,AUGUST,10))
                .crCardNm(1234567L)
                .build();
        phoebe = new GuestFactory().name("phoebe")
                .address("5th Avenue 21, New York")
                .birthday(LocalDate.of(1974,DECEMBER,5))
                .crCardNm(12345678L)
                .build();
        jefrey = new GuestFactory().name("jefrey")
                .address("Green Street 12, Springfield")
                .birthday(LocalDate.of(1987,MAY,30))
                .crCardNm(123456789L)
                .build();
        economy = new RoomFactory().name("Economy").price(200.00).capacity(3).build();
        luxury = new RoomFactory().name("Luxury").price(400.00).capacity(2).build();
        penthouse = new RoomFactory().name("Penthouse").price(2200.00).capacity(4).build();

        guestManager.createGuest(john);
        guestManager.createGuest(jane);
        guestManager.createGuest(jack);
        guestManager.createGuest(phoebe);
        guestManager.createGuest(jefrey);

        roomManager.createRoom(economy);
        roomManager.createRoom(luxury);
        roomManager.createRoom(penthouse);

        guestNotInDB = new GuestFactory().id(john.getId()+256).build();
    }

    @Test
    public void findGuestByInvalidID() {
        assertThat(guestManager.findGuestById(guestNotInDB.getId())).isNull();
    }

    @Test
    public void createAccommodation() {

        assertThat(manager.findRoomByGuest(john)).isNull();
        assertThat(manager.findRoomByGuest(jane)).isNull();
        assertThat(manager.findRoomByGuest(jack)).isNull();
        assertThat(manager.findRoomByGuest(phoebe)).isNull();
        assertThat(manager.findRoomByGuest(jefrey)).isNull();

        Accommodation acc1 = acc1Builder().build();
        Accommodation acc2 = acc2Builder().build();

        manager.createAccommodation(acc1);
        manager.createAccommodation(acc2);

        assertThat(manager.findGuestByRoom(economy)).isEqualToComparingFieldByField(john);
        assertThat(manager.findGuestByRoom(luxury)).isEqualToComparingFieldByField(jane);
        assertThat(manager.findGuestByRoom(penthouse)).isNull();

        assertThat(manager.findRoomByGuest(phoebe)).isNull();
        assertThat(manager.findRoomByGuest(jefrey)).isNull();
        assertThat(manager.findRoomByGuest(jack)).isNull();
        assertThat(manager.findRoomByGuest(john)).isEqualToComparingFieldByField(economy);
        assertThat(manager.findRoomByGuest(jane)).isEqualToComparingFieldByField(luxury);
    }

    @Test
    public void createAccommodationWithNullGuest(){
        Accommodation acc = acc1Builder().guest(null).build();
        expectedException.expect(ValidationError.class);
        manager.createAccommodation(acc);
    }

    @Test
    public void createAccommodationWithNullRoom() {
        Accommodation acc = acc1Builder().room(null).build();
        expectedException.expect(ValidationError.class);
        manager.createAccommodation(acc);
    }

    @Test
    public void createAccommodationWithExistingId() {
        Accommodation acc = acc1Builder().id(42L).build();
        expectedException.expect(InvalidEntityException.class);
        manager.createAccommodation(acc);
    }

    @Test
    public void deleteAccommodation() {
        Accommodation acc1 = acc1Builder().build();
        Accommodation acc2 = acc2Builder().build();

        manager.createAccommodation(acc1);
        manager.createAccommodation(acc2);

        assertThat(manager.findRoomByGuest(john)).isEqualToComparingFieldByField(economy);
        assertThat(manager.findRoomByGuest(jane)).isEqualToComparingFieldByField(luxury);
        assertThat(manager.findRoomByGuest(jack)).isNull();
        assertThat(manager.findRoomByGuest(jefrey)).isNull();
        assertThat(manager.findRoomByGuest(phoebe)).isNull();

        manager.deleteAccommodation(acc1);

        assertThat(manager.findAccommodationById(acc1.getId())).isNull();
        assertThat(manager.findAccommodationById(acc2.getId())).isEqualTo(acc2);
    }

    @Test
    public void deleteAccommodationWithNullId() {
        Accommodation acc1 = acc1Builder().build();
        manager.createAccommodation(acc1);
        acc1.setId(null);
        expectedException.expect(InvalidEntityException.class);
        manager.deleteAccommodation(acc1);
    }

    @Test
    public void updateAccommodation() {
        Accommodation acc1 = acc1Builder().build();
        manager.createAccommodation(acc1);

        Accommodation acc2 = acc2Builder().build();
        manager.createAccommodation(acc2);

        Long acc1Id = acc1.getId();

        acc1 = manager.findAccommodationById(acc1Id);
        acc1.setGuest(jack);
        manager.updateAccommodation(acc1);
        assertThat(manager.findAccommodationById(acc1.getId())).isEqualToComparingFieldByField(acc1);

        acc1 = manager.findAccommodationById(acc1Id);
        acc1.setRoom(penthouse);
        manager.updateAccommodation(acc1);
        assertThat(manager.findAccommodationById(acc1.getId())).isEqualToComparingFieldByField(acc1);

        acc1 = manager.findAccommodationById(acc1Id);
        acc1.setDateFrom(LocalDate.of(2008,FEBRUARY,29));
        manager.updateAccommodation(acc1);
        assertThat(manager.findAccommodationById(acc1.getId())).isEqualToComparingFieldByField(acc1);

        acc1 = manager.findAccommodationById(acc1Id);
        acc1.setDateTo(LocalDate.of(2016,APRIL,4));
        manager.updateAccommodation(acc1);
        assertThat(manager.findAccommodationById(acc1.getId())).isEqualToComparingFieldByField(acc1);

        acc1 = manager.findAccommodationById(acc1Id);
        acc1.setTotalPrice(201.00);
        manager.updateAccommodation(acc1);
        assertThat(manager.findAccommodationById(acc1.getId())).isEqualToComparingFieldByField(acc1);

        assertThat(manager.findAccommodationById(acc2.getId())).isEqualToComparingFieldByField(acc2);
    }

    @Test
    public void updateAccommodationWithNullId() {
        Accommodation acc1 = acc1Builder().build();
        manager.createAccommodation(acc1);
        acc1.setId(null);
        expectedException.expect(InvalidEntityException.class);
        manager.updateAccommodation(acc1);
    }

    @Test
    public void updateAccommodationWithNullGuest() {
        Accommodation acc1 = acc1Builder().build();
        manager.createAccommodation(acc1);
        acc1.setGuest(null);
        expectedException.expect(ValidationError.class);
        manager.updateAccommodation(acc1);
    }

    @Test
    public void updateAccommodationWithNullRoom() {
        Accommodation acc2 = acc2Builder().build();
        manager.createAccommodation(acc2);
        acc2.setRoom(null);
        expectedException.expect(ValidationError.class);
        manager.updateAccommodation(acc2);
    }

    @Test
    public void findAccommodationById() {
        Accommodation acc1 = acc1Builder().build();
        manager.createAccommodation(acc1);
        Accommodation accRetrieved = manager.findAccommodationById(acc1.getId());
        assertThat(accRetrieved).isEqualToComparingFieldByField(acc1);
    }

    @Test
    public void findAllAccommodations() {
        Accommodation acc1 = acc1Builder().guest(jefrey).build();
        manager.createAccommodation(acc1);
        Accommodation acc2 = acc1Builder().totalPrice(750.13).build();
        manager.createAccommodation(acc2);
        Accommodation acc3 = acc2Builder().room(penthouse).guest(phoebe).build();
        manager.createAccommodation(acc3);

        List<Accommodation> expectedAccommodations = new ArrayList<>();
        expectedAccommodations.add(acc1);
        expectedAccommodations.add(acc2);
        expectedAccommodations.add(acc3);
        assertThat(manager.findAllAccommodations())
                .usingFieldByFieldElementComparator()
                .isEqualTo(expectedAccommodations);
    }

    private AccommodationFactory acc1Builder() {
        return new AccommodationFactory().guest(john)
                .dateFrom(LocalDate.of(2016,FEBRUARY,28))
                .dateTo(LocalDate.of(2016,MARCH,1))
                .room(economy)
                .totalPrice(200.00);
    }

    private AccommodationFactory acc2Builder() {
        return new AccommodationFactory().guest(jane)
                .dateFrom(LocalDate.of(2016,FEBRUARY,27))
                .dateTo(LocalDate.of(2016,MARCH,4))
                .room(luxury)
                .totalPrice(400.00);
    }

}