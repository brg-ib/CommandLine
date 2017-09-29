package commandLineMenus;

import commandLineMenus.interfaces.Action;

import commandLineMenus.interfaces.ListAction;
import commandLineMenus.interfaces.ListItemRenderer;
import commandLineMenus.interfaces.ListData;
import commandLineMenus.interfaces.ListOption;
import commandLineMenus.rendering.examples.ListItemDefaultRenderer;

/**
 * Liste de valeurs (de type T) dans laquelle l'utilisateur
 * doit faire une sélection. Les valeurs de trouvant dans le champs
 * {@link liste} sont affichées et l'utilisateur est invité à saisir
 * l'indice de l'élément qu'il souhaite. Par défaut, la méthode toString
 * héritée de Object est utilisée pour afficher les éléments de menu, 
 * mais vous pouvez le modifier en utilisant la méthode 
 * {@link setToString}.
 */

public class List<T> extends Menu
{
	private ListAction<T> listAction = null;
	private ListOption<T> listOption = null;
	private ListData<T> model = null;
	private Option optionQuit = null, optionBack = null;
	private ListItemRenderer<T> itemRenderer;
	
	private List(String titre, ListData<T> model)
	{
		super(titre);
		this.model = model;
		setAutoBack(true);
		setListItemRenderer(new ListItemDefaultRenderer<>());
	}
	
	/**
	 * Créée une liste.
	 * @param titre intitulé affiché au dessus-de la liste.
	 * @param action l'objet permettant de gérer la liste.
	 */
	
	public List(String titre, ListData<T> model, ListAction<T> action)
	{
		this(titre, model);
		this.listAction = action;
	}
	
	/**
	 * Créée une liste.
	 * @param titre intitulé affiché au dessus-de la liste.
	 * @param action l'objet permettant de gérer la liste.
	 */
	
	public List(String titre, ListData<T> model, ListOption<T> option)
	{
		this(titre, model);
		this.listOption = option;
	}
	
	/**
	 * Créée une liste.
	 * @param titre intitulé affiché au dessus-de la liste.
	 * @param action l'objet permettant de gérer la liste.
	 * @param raccourci raccourci utilisé dans le cas où cette liste est utilisé comme option dans un menu.
	 */
	
	public List(String titre, String raccourci, ListData<T> model, ListAction<T> action)
	{
		this(titre, model, action);
		this.shortcut = raccourci;
	}
	
	/**
	 * Créée une liste.
	 * @param titre intitulé affiché au dessus-de la liste.
	 * @param option l'objet permettant de gérer la liste.
	 * @param raccourci raccourci utilisé dans le cas où cette liste est utilisé comme option dans un menu.
	 */
	
	public List(String titre, String raccourci, ListData<T> model, ListOption<T> option)
	{
		this(titre, model, option);
		this.shortcut = raccourci;
	}
	
	private Action getAction(final int indice, final T element)
	{
		return new Action()
		{
			@Override
			public void optionSelected()
			{
				selectedItem(indice, element);
			}
		};
	}

	private void selectedItem(int indice, T element)
	{
		if (listOption != null)
			super.optionSelected();
		if (listAction != null)
			listAction.itemSelected(indice, element);
	}
	
	private void add(int index, T element)
	{
		if (listAction != null)
			super.add(new Option(itemRenderer.title(index, element), 
					itemRenderer.shortcut(index, element),
					getAction(index, element))) ;
		if (listOption != null)
		{
			Option option = listOption.getOption(element);
			option.setShortcut(itemRenderer.shortcut(index, element));
			super.add(option);
		}
	}
	
	/**
	 * Déclenche une erreur, il est interdit de modifier les options d'une Liste.
	 */
	
	@Override
	public void add(Option option)
	{
		throw new ManualOptionAddForbiddenException(this, option);
	}
	
	@Override
	public void addQuit(String raccourci)
	{
		if (isLocked())
			throw new ConcurrentModificationException("Impossible to add \"quit\" option in list \"" 
					+ getTitle() + "\" while running.");
		this.optionQuit = new Option("Exit", raccourci, Action.QUIT);
	}

	@Override
	public void addBack(String raccourci)
	{
		if (isLocked())
			throw new ConcurrentModificationException("Impossible to add backoption in list " 
					+ getTitle() + " while running.");
		optionBack = new Option("Back", raccourci, Action.BACK);
	}
	
	public ListAction<T> getListAction()
	{
		return listAction;
	}
	
	public ListOption<T> getListOption()
	{
		return listOption;
	}
	
	@Override
	protected int actualize()
	{
		java.util.List<T> liste = model.getList();
		if (liste == null)
			throw new NoListModelDefinedException(this);
		clearOptions();
		boolean wasLocked = unlock();
		for (int i = 0 ; i < liste.size() ; i++)
			add(i, liste.get(i));
		if (optionQuit != null)
			super.add(optionQuit);
		if (optionBack!= null)
			super.add(optionBack);
		setLocked(wasLocked);
		return liste.size();
	}
	
	@Override
	protected Option runOnce()
	{
		int nbOptions = actualize();
		if (nbOptions == 0)
			menuRenderer.outputString(itemRenderer.empty());
		else
		{
			new DepthFirstSearch(this);
		}
		return super.runOnce();
	}	
	
	/**
	 * Définit de quelle façon vont s'afficher les éléments de menu.
	 */
	
	public void setListItemRenderer(ListItemRenderer<T> convertisseur)
	{
		if (isLocked())
			throw new ConcurrentModificationException("Impossible to change renderer of list " 
					+ getTitle() + " while running.");
		this.itemRenderer = convertisseur;
	}

	public static class ManualOptionAddForbiddenException extends RuntimeException
	{
		private static final long serialVersionUID = -5126287607702961669L;
		private Option option;
		private List<?> list;

		ManualOptionAddForbiddenException(List<?> list, Option option)
		{
			this.list = list;
			this.option = option;
		}
		
		@Override
		public String toString()
		{
			return "It is forbidden to manually add an option (ie. : " + option.getTitle() +
					") in a list (ie. : " + list.getTitle() + ").";
		}
	}

	public static class NoListModelDefinedException extends RuntimeException
	{
		private static final long serialVersionUID = 3072039179151217765L;
		private List<?> list;
		
		public NoListModelDefinedException(List<?> list)
		{
			this.list = list;
		}
		
		@Override
		public String toString()
		{
			return "No list model defined for list " + list.getTitle() + ").";
		}
	}

	public static class NoListActionDefinedException extends RuntimeException
	{
		private static final long serialVersionUID = -4035301642069764296L;
		private List<?> list;
		
		public NoListActionDefinedException(List<?> list)
		{
			this.list = list;
		}
		
		@Override
		public String toString()
		{
			return "No list action defined for list " + list.getTitle() + ").";
		}
	}
}
	
